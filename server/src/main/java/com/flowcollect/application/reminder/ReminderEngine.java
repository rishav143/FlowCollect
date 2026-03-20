package com.flowcollect.application.reminder;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.domain.invoice.followup.FollowUp;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.organization.OrganizationStatus;

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
 *
 * <p>Each organization is processed in its own {@code REQUIRES_NEW} transaction
 * (via {@link ReminderOrgProcessor}) so that a failure for one org cannot poison
 * and roll back work already done for other organizations.
 */
@Service
public class ReminderEngine {

    private static final Logger log = LoggerFactory.getLogger(ReminderEngine.class);

    private static final List<OrganizationStatus> ELIGIBLE_ORGANIZATION_STATUSES = List.of(
            OrganizationStatus.ACTIVE,
            OrganizationStatus.TRIAL
    );

    private final OrganizationService organizationService;
    private final ReminderOrgProcessor orgProcessor;

    public ReminderEngine(
            OrganizationService organizationService,
            ReminderOrgProcessor orgProcessor
    ) {
        this.organizationService = organizationService;
        this.orgProcessor = orgProcessor;
    }

    public void runAutomatedReminders() {
        List<Organization> organizations = organizationService.getEligibleOrganizationsForReminders(
                ELIGIBLE_ORGANIZATION_STATUSES
        );

        int totalCreated = 0;
        int totalDispatched = 0;
        int totalCancelled = 0;

        for (Organization organization : organizations) {
            try {
                ReminderResult result = orgProcessor.process(organization);
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
}
