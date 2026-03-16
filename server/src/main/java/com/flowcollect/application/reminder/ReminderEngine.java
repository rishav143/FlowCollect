package com.flowcollect.application.reminder;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowcollect.application.invoice.FollowUpService;
import com.flowcollect.application.invoice.InvoiceService;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.LifeCycleStatus;
import com.flowcollect.domain.invoice.followup.FollowUp;
import com.flowcollect.domain.invoice.followup.FollowUpChannel;
import com.flowcollect.domain.invoice.followup.FollowUpStatus;
import com.flowcollect.domain.invoice.followup.FollowUpTriggerType;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.organization.OrganizationStatus;
import com.flowcollect.domain.reminder.ReminderRule;
import com.flowcollect.domain.reminder.ReminderTriggerType;
import com.flowcollect.domain.template.TemplateChannel;

/**
 * Orchestrates automated payment reminders for all eligible organizations.
 *
 * <p>Each scheduler run executes two sequential phases per organization:
 * <ol>
 *   <li><b>Schedule:</b> For each active reminder rule, find invoices whose due date
 *       matches the rule's target date and create a PENDING {@link FollowUp} if one
 *       doesn't already exist for that invoice + rule + occurrence combination.</li>
 *   <li><b>Dispatch:</b> Send all PENDING automated follow-ups that are due today,
 *       cancelling any that are stale or whose invoices are no longer eligible.</li>
 * </ol>
 *
 * <p>The two phases are intentionally separate: follow-ups created in Phase 1 are
 * not dispatched until the <em>next</em> scheduler run. This is because
 * {@link FollowUpService#dispatchFollowUp(FollowUp)} runs in a new transaction
 * ({@code REQUIRES_NEW}) and cannot see rows created in the current uncommitted
 * outer transaction.
 */
@Service
public class ReminderEngine {

    private static final Logger log = LoggerFactory.getLogger(ReminderEngine.class);

    private static final List<OrganizationStatus> ELIGIBLE_ORGANIZATION_STATUSES = List.of(
            OrganizationStatus.ACTIVE,
            OrganizationStatus.TRIAL
    );

    private static final List<LifeCycleStatus> ELIGIBLE_INVOICE_STATUSES = List.of(
            LifeCycleStatus.ISSUED,
            LifeCycleStatus.PARTIALLY_PAID
    );

    private final OrganizationService organizationService;
    private final ReminderRuleService reminderRuleService;
    private final InvoiceService invoiceService;
    private final FollowUpService followUpService;

    /**
     * Max age in days a PENDING follow-up may sit before being cancelled instead of dispatched.
     * Prevents a burst of stale messages going out after a scheduler outage.
     * Configured via {@code scheduler.reminder.max-pending-age-days} (default: 1).
     * Set to 0 to disable the staleness check entirely.
     */
    private final int maxPendingAgeDays;

    public ReminderEngine(
            OrganizationService organizationService,
            ReminderRuleService reminderRuleService,
            InvoiceService invoiceService,
            FollowUpService followUpService,
            @Value("${scheduler.reminder.max-pending-age-days:1}") int maxPendingAgeDays
    ) {
        this.organizationService = organizationService;
        this.reminderRuleService = reminderRuleService;
        this.invoiceService = invoiceService;
        this.followUpService = followUpService;
        this.maxPendingAgeDays = maxPendingAgeDays;
    }

    // =========================================================================
    // Public entry point
    // =========================================================================

    @Transactional
    public void runAutomatedReminders() {
        List<Organization> organizations = organizationService.getEligibleOrganizationsForReminders(
                ELIGIBLE_ORGANIZATION_STATUSES
        );

        int totalCreated = 0;
        int totalDispatched = 0;
        int totalCancelled = 0;

        for (Organization organization : organizations) {
            try {
                ReminderResult result = processOrganization(organization);
                totalCreated    += result.created();
                totalDispatched += result.dispatched();
                totalCancelled  += result.cancelled();
            } catch (Exception ex) {
                log.error("[org={}] Reminder processing failed — skipping", organization.getId(), ex);
            }
        }

        log.info("Reminder run complete. created={}, dispatched={}, cancelled={}",
                totalCreated, totalDispatched, totalCancelled);
    }

    // =========================================================================
    // Per-organization orchestration
    // =========================================================================

    private ReminderResult processOrganization(Organization organization) {
        int created            = scheduleFollowUps(organization);
        ReminderResult dispatch = dispatchPendingFollowUps(organization);
        return new ReminderResult(created, dispatch.dispatched(), dispatch.cancelled());
    }

    // =========================================================================
    // Phase 1: Schedule (create) follow-ups
    // =========================================================================

    /**
     * Creates PENDING follow-ups for every rule × invoice × occurrence combination
     * that does not already have one. Runs entirely inside the caller's transaction.
     */
    private int scheduleFollowUps(Organization organization) {
        LocalDate today = LocalDate.now(organization.getTimezone());
        int created = 0;

        for (ReminderRule rule : reminderRuleService.getActiveReminderRules(organization.getId())) {
            created += scheduleFollowUpsForRule(organization, rule, today);
        }
        return created;
    }

    private int scheduleFollowUpsForRule(Organization organization, ReminderRule rule, LocalDate today) {
        if (!isSchedulable(rule)) {
            log.warn("[org={}] Skipping invalid reminder rule {} ('{}')",
                    organization.getId(), rule.getId(), rule.getName());
            return 0;
        }
        // Honor the optional rule start date (evaluated in the organization's timezone).
        if (rule.getStartDate() != null && today.isBefore(rule.getStartDate())) {
            return 0;
        }

        int created = 0;
        // Iterate occurrences (0-based). Non-cyclic rules have maxOccurrences == 1,
        // so this loop runs exactly once with occurrence index 0.
        for (int occurrence = 0; occurrence < rule.getMaxOccurrences(); occurrence++) {
            created += scheduleFollowUpsForOccurrence(organization, rule, occurrence, today);
        }
        return created;
    }

    private int scheduleFollowUpsForOccurrence(
            Organization organization, ReminderRule rule, int occurrence, LocalDate today) {

        LocalDate targetDueDate = computeTargetDueDate(rule, occurrence, today);
        int created = 0;

        for (Invoice invoice : invoiceService.getInvoicesForReminders(
                organization.getId(), ELIGIBLE_INVOICE_STATUSES, targetDueDate)) {

            // Skip if this occurrence has already been created for this invoice + rule pair.
            if (followUpService.existsByInvoiceIdAndReminderRuleIdAndOccurrenceIndex(
                    invoice.getId(), rule.getId(), occurrence)) {
                continue;
            }

            followUpService.save(buildFollowUp(invoice, rule, occurrence, today));
            created++;
        }
        return created;
    }

    /**
     * Computes the invoice due date this rule occurrence targets on a given scheduler day.
     *
     * <p>The effective offset grows across occurrences for cyclic rules:
     * {@code effectiveOffset = daysOffset + (cycleIntervalDays × occurrenceIndex)}.
     *
     * <ul>
     *   <li>{@code BEFORE_DUE_DATE}: {@code daysOffset} is negative. We take its absolute
     *       value and add to today, targeting invoices due that many days <em>from now</em>.</li>
     *   <li>{@code ON_DUE_DATE}: always today ({@code daysOffset == 0}, never cyclic).</li>
     *   <li>{@code AFTER_DUE_DATE}: {@code daysOffset} is positive. We subtract it from today,
     *       targeting invoices that were due that many days <em>ago</em>.</li>
     * </ul>
     *
     * <p>{@link #isSchedulable(ReminderRule)} guarantees that for BEFORE_DUE_DATE rules the
     * accumulated offset never crosses zero, so {@code Math.abs(effectiveOffset)} is always
     * equivalent to {@code -effectiveOffset} across all occurrences.
     */
    private LocalDate computeTargetDueDate(ReminderRule rule, int occurrence, LocalDate today) {
        int effectiveOffset = rule.getDaysOffset() + (rule.getCycleIntervalDays() * occurrence);
        return switch (rule.getTriggerType()) {
            case BEFORE_DUE_DATE -> today.plusDays(Math.abs(effectiveOffset));
            case ON_DUE_DATE     -> today;
            case AFTER_DUE_DATE  -> today.minusDays(effectiveOffset);
        };
    }

    private FollowUp buildFollowUp(Invoice invoice, ReminderRule rule, int occurrence, LocalDate scheduledDate) {
        FollowUp followUp = new FollowUp();
        followUp.setInvoice(invoice);
        followUp.setTemplate(rule.getTemplate());
        followUp.setReminderRule(rule);
        followUp.setChannel(FollowUpChannel.valueOf(rule.getChannel().name()));
        followUp.setTriggerType(FollowUpTriggerType.AUTOMATED);
        followUp.setStatus(FollowUpStatus.PENDING);
        followUp.setScheduledForDate(scheduledDate);
        followUp.setAttachPdf(rule.isAttachPdf());
        followUp.setOccurrenceIndex(occurrence);
        return followUp;
    }

    // =========================================================================
    // Phase 2: Dispatch pending follow-ups
    // =========================================================================

    /**
     * Processes all PENDING automated follow-ups for the organization.
     * Each follow-up is either dispatched (sent), cancelled, or skipped (not yet due).
     */
    private ReminderResult dispatchPendingFollowUps(Organization organization) {
        LocalDate today = LocalDate.now(organization.getTimezone());
        int dispatched = 0;
        int cancelled  = 0;

        for (FollowUp followUp : followUpService.getPendingAutomatedFollowUps(organization.getId())) {

            // --- Staleness check ---
            // Cancel follow-ups that have been PENDING too long without being dispatched.
            // This prevents a burst of outdated reminders reaching customers after a
            // scheduler outage. Configurable via scheduler.reminder.max-pending-age-days.
            if (maxPendingAgeDays > 0 && isStale(followUp, today)) {
                log.info("[org={}] Cancelling stale follow-up {} (scheduledFor={}, today={})",
                        organization.getId(), followUp.getId(), followUp.getScheduledForDate(), today);
                followUpService.cancelFollowUp(followUp);
                cancelled++;
                continue;
            }

            if (!isDueForDispatch(followUp, today)) {
                continue;
            }

            // --- Invoice eligibility check ---
            // Re-check the invoice's lifecycle status before dispatching. The invoice may
            // have been fully paid or cancelled since this follow-up was first created.
            // We cancel (not fail) because no delivery was attempted — it's simply no
            // longer needed. This avoids polluting FAILED counts with legitimate outcomes.
            Invoice invoice = followUp.getInvoice();
            if (invoice == null || !isInvoiceEligible(invoice)) {
                log.info("[org={}] Cancelling follow-up {} — invoice {} is no longer eligible (status={})",
                        organization.getId(), followUp.getId(),
                        invoice != null ? invoice.getId() : "null",
                        invoice != null ? invoice.getLifeCycleStatus() : "null");
                followUpService.cancelFollowUp(followUp);
                cancelled++;
                continue;
            }

            // --- Dispatch ---
            FollowUp result = tryDispatch(followUp);
            if (result != null && result.isSent()) {
                dispatched++;
            }
        }

        return new ReminderResult(0, dispatched, cancelled);
    }

    private boolean isDueForDispatch(FollowUp followUp, LocalDate today) {
        LocalDate scheduledDate = followUp.getScheduledForDate();
        if (scheduledDate == null) {
            log.warn("[followUp={}] scheduledForDate is null — dispatching immediately (rule={})",
                    followUp.getId(),
                    followUp.getReminderRule() != null ? followUp.getReminderRule().getId() : "none");
            return true;
        }
        return !scheduledDate.isAfter(today);
    }

    /**
     * Returns true if a PENDING follow-up has been waiting longer than
     * {@code maxPendingAgeDays} without being dispatched.
     */
    private boolean isStale(FollowUp followUp, LocalDate today) {
        LocalDate scheduled = followUp.getScheduledForDate();
        if (scheduled == null) {
            return false; // No date: let isDueForDispatch handle it.
        }
        return scheduled.plusDays(maxPendingAgeDays).isBefore(today);
    }

    private boolean isInvoiceEligible(Invoice invoice) {
        return ELIGIBLE_INVOICE_STATUSES.contains(invoice.getLifeCycleStatus());
    }

    private FollowUp tryDispatch(FollowUp followUp) {
        try {
            return followUpService.dispatchFollowUp(followUp);
        } catch (RuntimeException ex) {
            log.warn("[followUp={}] Dispatch failed", followUp.getId(), ex);
            return followUp;
        }
    }

    // =========================================================================
    // Rule validation
    // =========================================================================

    /**
     * Returns true when a rule is safe to schedule against invoices.
     *
     * <ul>
     *   <li>Rule and its template must both be active.</li>
     *   <li>Channel must match the template's channel.</li>
     *   <li>{@code BEFORE_DUE_DATE}: offset must be negative. For cyclic rules, the
     *       accumulated offset must remain negative across <em>all</em> occurrences
     *       ({@code cycleIntervalDays × (maxOccurrences - 1) < |daysOffset|}), otherwise
     *       later occurrences would silently target wrong due dates.</li>
     *   <li>{@code ON_DUE_DATE}: offset must be zero; cyclic is rejected because all
     *       occurrences would target the same invoices and fire simultaneously.</li>
     *   <li>{@code AFTER_DUE_DATE}: offset must be positive. Cyclic rules require a
     *       positive interval so successive occurrences target increasingly overdue invoices.</li>
     * </ul>
     */
    private boolean isSchedulable(ReminderRule rule) {
        if (rule == null || !rule.isActive()) {
            return false;
        }
        if (rule.getTemplate() == null || !rule.getTemplate().isActive()) {
            return false;
        }
        if (rule.getChannel() == null || rule.getTriggerType() == null) {
            return false;
        }
        if (rule.getTemplate().getChannel() != TemplateChannel.valueOf(rule.getChannel().name())) {
            return false;
        }
        if (rule.getMaxOccurrences() < 1) {
            return false;
        }
        return switch (rule.getTriggerType()) {
            case BEFORE_DUE_DATE -> rule.getDaysOffset() < 0
                    && (!rule.isCyclic() || (
                        rule.getCycleIntervalDays() > 0
                        && (long) rule.getCycleIntervalDays() * (rule.getMaxOccurrences() - 1) < Math.abs(rule.getDaysOffset())
                    ));
            case ON_DUE_DATE -> rule.getDaysOffset() == 0 && !rule.isCyclic();
            case AFTER_DUE_DATE -> rule.getDaysOffset() > 0
                    && (!rule.isCyclic() || rule.getCycleIntervalDays() > 0);
        };
    }

    // =========================================================================
    // Internal types
    // =========================================================================

    private record ReminderResult(int created, int dispatched, int cancelled) {}
}

