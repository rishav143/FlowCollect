package com.flowcollect.application.reminder;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.flowcollect.domain.template.TemplateChannel;

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

    public ReminderEngine(
            OrganizationService organizationService,
            ReminderRuleService reminderRuleService,
            InvoiceService invoiceService,
            FollowUpService followUpService
    ) {
        this.organizationService = organizationService;
        this.reminderRuleService = reminderRuleService;
        this.invoiceService = invoiceService;
        this.followUpService = followUpService;
    }

    @Transactional
    public void runAutomatedReminders() {
        int createdCount = 0;
        int dispatchedCount = 0;

        List<Organization> organizations = organizationService.getEligibleOrganizationsForReminders(
                ELIGIBLE_ORGANIZATION_STATUSES
        );
        for (Organization organization : organizations) {
            createdCount += createAutomatedFollowUpsForOrganization(organization);
            dispatchedCount += dispatchPendingAutomatedFollowUpsForOrganization(organization);
        }

        log.info("Reminder scheduler completed. created={}, dispatched={}", createdCount, dispatchedCount);
    }

    private int createAutomatedFollowUpsForOrganization(Organization organization) {
        LocalDate organizationToday = LocalDate.now(organization.getTimezone());
        int createdCount = 0;

        List<ReminderRule> activeRules = reminderRuleService.getActiveReminderRules(organization.getId());
        for (ReminderRule rule : activeRules) {
            if (!isSchedulable(rule)) {
                log.warn("Skipping invalid reminder rule {} for organization {}", rule.getId(), organization.getId());
                continue;
            }

            // Honor the optional custom start date (evaluated in the organization's timezone).
            if (rule.getStartDate() != null && organizationToday.isBefore(rule.getStartDate())) {
                continue;
            }

            // Iterate over each occurrence (0-based). For non-cyclic rules maxOccurrences == 1,
            // so the loop runs exactly once with occurrence index 0.
            for (int occurrence = 0; occurrence < rule.getMaxOccurrences(); occurrence++) {
                // Each successive occurrence targets invoices that are further past their due date.
                int effectiveOffset = rule.getDaysOffset() + (rule.getCycleIntervalDays() * occurrence);
                LocalDate targetDueDate = organizationToday.minusDays(effectiveOffset);

                List<Invoice> invoices = invoiceService.getInvoicesForReminders(
                        organization.getId(),
                        ELIGIBLE_INVOICE_STATUSES,
                        targetDueDate
                );

                for (Invoice invoice : invoices) {
                    // Skip if this occurrence has already been created for this invoice+rule pair.
                    if (followUpService.existsByInvoiceIdAndReminderRuleIdAndOccurrenceIndex(
                            invoice.getId(),
                            rule.getId(),
                            occurrence
                    )) {
                        continue;
                    }

                    FollowUp followUp = new FollowUp();
                    followUp.setInvoice(invoice);
                    followUp.setTemplate(rule.getTemplate());
                    followUp.setReminderRule(rule);
                    followUp.setChannel(FollowUpChannel.valueOf(rule.getChannel().name()));
                    followUp.setTriggerType(FollowUpTriggerType.AUTOMATED);
                    followUp.setStatus(FollowUpStatus.PENDING);
                    followUp.setScheduledForDate(organizationToday);
                    followUp.setAttachPdf(rule.isAttachPdf());
                    followUp.setOccurrenceIndex(occurrence);

                    followUpService.save(followUp);
                    createdCount++;
                }
            }
        }

        return createdCount;
    }

    private int dispatchPendingAutomatedFollowUpsForOrganization(Organization organization) {
        LocalDate organizationToday = LocalDate.now(organization.getTimezone());
        int dispatchedCount = 0;

        List<FollowUp> pendingFollowUps = followUpService.getPendingAutomatedFollowUps(
                organization.getId()
        );

        for (FollowUp followUp : pendingFollowUps) {
            if (!isDueForDispatch(followUp, organizationToday)) {
                continue;
            }
            if (dispatch(followUp).isSent()) {
                dispatchedCount++;
            }
        }

        return dispatchedCount;
    }

    private boolean isDueForDispatch(FollowUp followUp, LocalDate organizationToday) {
        LocalDate scheduledForDate = followUp.getScheduledForDate();
        return scheduledForDate == null || !scheduledForDate.isAfter(organizationToday);
    }

    private boolean isSchedulable(ReminderRule rule) {
        if (rule == null || !rule.isActive() || rule.getTemplate() == null || !rule.getTemplate().isActive()) {
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
        // A cyclic rule must define a positive interval; otherwise occurrences would all target the same due date.
        if (rule.isCyclic() && rule.getCycleIntervalDays() < 1) {
            return false;
        }

        // Ensure the base daysOffset sign matches the triggerType
        return switch (rule.getTriggerType()) {
            case BEFORE_DUE_DATE -> rule.getDaysOffset() <= 0;
            case ON_DUE_DATE -> rule.getDaysOffset() == 0;
            case AFTER_DUE_DATE -> rule.getDaysOffset() >= 0;
        };
    }

    private FollowUp dispatch(FollowUp followUp) {
        if (followUp == null || !followUp.isPending()) {
            return followUp;
        }

        try {
            return followUpService.dispatchFollowUp(followUp);
        } catch (RuntimeException ex) {
            log.warn("Failed to dispatch follow-up {}", followUp.getId(), ex);
            return followUp;
        }
    }

}
