package com.flowcollect.application.reminder;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.flowcollect.application.invoice.FollowUpService;
import com.flowcollect.application.invoice.InvoiceService;
import com.flowcollect.application.template.TemplateService;
import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.LifeCycleStatus;
import com.flowcollect.domain.invoice.followup.FollowUp;
import com.flowcollect.domain.template.Template;
import com.flowcollect.domain.invoice.followup.FollowUpChannel;
import com.flowcollect.domain.invoice.followup.FollowUpStatus;
import com.flowcollect.domain.invoice.followup.FollowUpTriggerType;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.reminder.ReminderRule;
import com.flowcollect.domain.reminder.RuleMode;
import com.flowcollect.domain.template.TemplateChannel;

/**
 * Handles per-organization reminder scheduling and dispatch in an isolated transaction.
 * Running each org in {@code REQUIRES_NEW} ensures that a failure or rollback for one org
 * (e.g. a constraint violation from a concurrent scheduler instance) does not poison the
 * outer transaction and roll back work done for other organizations.
 */
@Service
public class ReminderOrgProcessor {

    private static final Logger log = LoggerFactory.getLogger(ReminderOrgProcessor.class);

    private static final List<LifeCycleStatus> ELIGIBLE_INVOICE_STATUSES = List.of(
            LifeCycleStatus.ISSUED,
            LifeCycleStatus.PARTIALLY_PAID
    );

    private final ReminderRuleService reminderRuleService;
    private final InvoiceService invoiceService;
    private final FollowUpService followUpService;
    private final TemplateService templateService;
    private final int maxPendingAgeDays;

    public ReminderOrgProcessor(
            ReminderRuleService reminderRuleService,
            InvoiceService invoiceService,
            FollowUpService followUpService,
            TemplateService templateService,
            @org.springframework.beans.factory.annotation.Value("${scheduler.reminder.max-pending-age-days:1}") int maxPendingAgeDays
    ) {
        this.reminderRuleService = reminderRuleService;
        this.invoiceService = invoiceService;
        this.followUpService = followUpService;
        this.templateService = templateService;
        this.maxPendingAgeDays = maxPendingAgeDays;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReminderResult process(Organization organization) {
        int created          = scheduleFollowUps(organization);
        ReminderResult dispatch = dispatchPendingFollowUps(organization);
        return new ReminderResult(created, dispatch.dispatched(), dispatch.cancelled());
    }

    // =========================================================================
    // Phase 1: Schedule (create) follow-ups
    // =========================================================================

    private int scheduleFollowUps(Organization organization) {
        LocalDate today = LocalDate.now(organization.getTimezone());
        int created = 0;

        // Org-owned MANUAL rules always run
        for (ReminderRule rule : reminderRuleService.getActiveReminderRules(organization.getId())) {
            if (rule.getMode() == RuleMode.MANUAL) {
                created += scheduleFollowUpsForRule(organization, rule, today);
            }
        }

        // AUTO rules (org-owned + system-defined) only run when Recover is enabled on a PRO org.
        // Handled separately to include system-seeded rules (organization = null).
        if (organization.isAutoRecoveryEnabled() && organization.getPlan() == com.flowcollect.domain.organization.OrgPlan.PRO) {
            for (ReminderRule rule : reminderRuleService.getActiveAutoRulesForOrg(organization.getId())) {
                created += scheduleFollowUpsForRule(organization, rule, today);
            }
        }

        return created;
    }

    private int scheduleFollowUpsForRule(Organization organization, ReminderRule rule, LocalDate today) {
        if (!isSchedulable(rule)) {
            log.warn("[org={}] Skipping invalid reminder rule {} ('{}')",
                    organization.getId(), rule.getId(), rule.getName());
            return 0;
        }
        if (rule.getStartDate() != null && today.isBefore(rule.getStartDate())) {
            return 0;
        }
        int created = 0;
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

            Customer customer = invoice.getCustomer();
            if (customer == null || !customer.isActive() || !customer.isAutomationEnabled()) {
                continue;
            }
            if (followUpService.existsByInvoiceIdAndReminderRuleIdAndOccurrenceIndex(
                    invoice.getId(), rule.getId(), occurrence)) {
                continue;
            }
            followUpService.save(buildFollowUp(invoice, rule, occurrence, today));
            created++;
        }
        return created;
    }

    private LocalDate computeTargetDueDate(ReminderRule rule, int occurrence, LocalDate today) {
        int effectiveOffset = rule.getDaysOffset() + (rule.getCycleIntervalDays() * occurrence);
        return switch (rule.getTriggerType()) {
            case BEFORE_DUE_DATE -> today.plusDays(Math.abs(effectiveOffset));
            case ON_DUE_DATE     -> today;
            case AFTER_DUE_DATE  -> today.minusDays(effectiveOffset);
        };
    }

    private Template resolveTemplate(ReminderRule rule, int occurrence) {
        java.util.List<java.util.UUID> occIds = rule.getOccurrenceTemplateIds();
        if (occIds != null && occurrence < occIds.size()) {
            java.util.UUID tplId = occIds.get(occurrence);
            if (tplId != null) {
                try {
                    return templateService.getTemplateById(tplId);
                } catch (RuntimeException ex) {
                    log.warn("[rule={}] Could not load occurrence template at index {} (id={}), falling back to primary",
                            rule.getId(), occurrence, tplId);
                }
            }
        }
        return rule.getTemplate();
    }

    private FollowUp buildFollowUp(Invoice invoice, ReminderRule rule, int occurrence, LocalDate scheduledDate) {
        FollowUp followUp = new FollowUp();
        followUp.setInvoice(invoice);
        followUp.setTemplate(resolveTemplate(rule, occurrence));
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

    private ReminderResult dispatchPendingFollowUps(Organization organization) {
        LocalDate today = LocalDate.now(organization.getTimezone());
        int dispatched = 0;
        int cancelled  = 0;

        for (FollowUp followUp : followUpService.getPendingAutomatedFollowUps(organization.getId())) {
            // AUTO rule follow-ups must be cancelled when auto-recovery is disabled.
            // MANUAL rule follow-ups always proceed — they are independent of the toggle.
            if (!organization.isAutoRecoveryEnabled()) {
                ReminderRule rule = followUp.getReminderRule();
                boolean isAutoRule = rule != null && rule.getMode() == RuleMode.AUTO;
                if (isAutoRule) {
                    followUpService.cancelFollowUp(followUp);
                    log.info("[org={}] Auto-recovery disabled — cancelled queued AUTO follow-up {}",
                            organization.getId(), followUp.getId());
                    cancelled++;
                    continue;
                }
            }

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
            if (invoice.getCustomer() == null) {
                log.info("[org={}] Cancelling follow-up {} — invoice {} has no customer",
                        organization.getId(), followUp.getId(), invoice.getId());
                followUpService.cancelFollowUp(followUp);
                cancelled++;
                continue;
            }
            if (!invoice.getCustomer().isActive()) {
                log.info("[org={}] Cancelling follow-up {} — customer {} is archived",
                        organization.getId(), followUp.getId(), invoice.getCustomer().getId());
                followUpService.cancelFollowUp(followUp);
                cancelled++;
                continue;
            }
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

    private boolean isStale(FollowUp followUp, LocalDate today) {
        LocalDate scheduled = followUp.getScheduledForDate();
        if (scheduled == null) {
            return false;
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
}
