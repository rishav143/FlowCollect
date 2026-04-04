package com.flowcollect.application.recover;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.flowcollect.api.v1.recover.dto.ActivityItemResponse;
import com.flowcollect.api.v1.recover.dto.QueueActivityResponse;
import com.flowcollect.api.v1.recover.dto.UpcomingItemResponse;
import com.flowcollect.application.invoice.InvoiceService;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.LifeCycleStatus;
import com.flowcollect.domain.invoice.followup.FollowUp;
import com.flowcollect.domain.invoice.followup.FollowUpStatus;
import com.flowcollect.domain.reminder.ReminderRule;
import com.flowcollect.domain.reminder.ReminderTriggerType;
import com.flowcollect.infrastructure.persistence.invoice.FollowUpJpaRepository;
import com.flowcollect.infrastructure.persistence.reminder.ReminderRuleJpaRepository;

@Service
public class RecoverQueueService {

    private static final int UPCOMING_DISPLAY_LIMIT  = 10;
    private static final int UPCOMING_WINDOW_DAYS    = 7;
    private static final int ACTIVITY_DISPLAY_LIMIT  = 10;
    private static final int ACTIVITY_LOOKBACK_DAYS  = 7;

    private static final List<LifeCycleStatus> ELIGIBLE_STATUSES =
            List.of(LifeCycleStatus.ISSUED, LifeCycleStatus.PARTIALLY_PAID);

    private final FollowUpJpaRepository    followUpRepository;
    private final ReminderRuleJpaRepository reminderRuleRepository;
    private final InvoiceService            invoiceService;

    public RecoverQueueService(FollowUpJpaRepository followUpRepository,
                               ReminderRuleJpaRepository reminderRuleRepository,
                               InvoiceService invoiceService) {
        this.followUpRepository    = followUpRepository;
        this.reminderRuleRepository = reminderRuleRepository;
        this.invoiceService         = invoiceService;
    }

    public QueueActivityResponse getQueueAndActivity(UUID organizationId) {

        // ── Upcoming follow-ups (next 7 days) ─────────────────────────────────
        LocalDate today     = LocalDate.now();
        LocalDate windowEnd = today.plusDays(UPCOMING_WINDOW_DAYS);

        List<ReminderRule> rules    = reminderRuleRepository.findActiveAutoRulesForOrg(organizationId);
        List<Invoice>      invoices = invoiceService.getInvoicesWithDueDate(organizationId, ELIGIBLE_STATUSES);

        List<UpcomingItemResponse> allUpcoming = new ArrayList<>();

        for (ReminderRule rule : rules) {
            for (int occ = 0; occ < rule.getMaxOccurrences(); occ++) {
                int effectiveOffset = rule.getDaysOffset() + (rule.getCycleIntervalDays() * occ);

                for (Invoice invoice : invoices) {
                    if (invoice.getDueDate() == null) continue;
                    // Respect per-customer automation opt-out (same guard as scheduler)
                    if (invoice.getCustomer() != null && !invoice.getCustomer().isAutomationEnabled()) continue;

                    LocalDate scheduledDate = computeScheduledDate(rule.getTriggerType(), effectiveOffset, invoice.getDueDate());
                    if (scheduledDate == null) continue;
                    if (scheduledDate.isBefore(today) || scheduledDate.isAfter(windowEnd)) continue;

                    // Skip if this occurrence was already fired or is already queued
                    if (followUpRepository.existsByInvoiceIdAndReminderRuleIdAndOccurrenceIndex(
                            invoice.getId(), rule.getId(), occ)) continue;

                    int daysUntil = (int) ChronoUnit.DAYS.between(today, scheduledDate);
                    allUpcoming.add(new UpcomingItemResponse(
                            invoice.getId(),
                            invoice.getInvoiceNumber(),
                            invoice.getCustomer() != null ? invoice.getCustomer().getName() : "Unknown",
                            rule.getChannel(),
                            scheduledDate,
                            rule.getName(),
                            daysUntil
                    ));
                }
            }
        }

        allUpcoming.sort(Comparator.comparing(UpcomingItemResponse::getScheduledDate));
        long totalUpcoming = allUpcoming.size();
        List<UpcomingItemResponse> upcoming = allUpcoming.stream().limit(UPCOMING_DISPLAY_LIMIT).toList();

        // ── Activity log ──────────────────────────────────────────────────────
        Instant since = Instant.now().minus(ACTIVITY_LOOKBACK_DAYS, ChronoUnit.DAYS);
        List<FollowUp> recent = followUpRepository
                .findRecentAutoActivity(organizationId, since, PageRequest.of(0, ACTIVITY_DISPLAY_LIMIT))
                .getContent();

        List<ActivityItemResponse> activity = recent.stream()
                .map(f -> {
                    Instant eventAt = f.getStatus() == FollowUpStatus.SENT
                            ? (f.getSentAt() != null ? f.getSentAt() : f.getUpdatedAt())
                            : f.getUpdatedAt();
                    String ruleName = f.getReminderRule() != null ? f.getReminderRule().getName() : null;
                    return new ActivityItemResponse(
                            f.getId(),
                            f.getInvoice().getInvoiceNumber(),
                            f.getInvoice().getCustomer() != null ? f.getInvoice().getCustomer().getName() : "Unknown",
                            f.getChannel(),
                            f.getStatus(),
                            ruleName,
                            eventAt
                    );
                })
                .toList();

        return new QueueActivityResponse(upcoming, totalUpcoming, activity);
    }

    /**
     * Inverse of ReminderOrgProcessor.computeTargetDueDate —
     * given an invoice's due date, returns the calendar day the scheduler would fire.
     */
    private LocalDate computeScheduledDate(ReminderTriggerType triggerType, int effectiveOffset, LocalDate dueDate) {
        return switch (triggerType) {
            case BEFORE_DUE_DATE -> dueDate.minusDays(Math.abs(effectiveOffset));
            case AFTER_DUE_DATE  -> dueDate.plusDays(effectiveOffset);
            case ON_DUE_DATE     -> dueDate;
        };
    }
}
