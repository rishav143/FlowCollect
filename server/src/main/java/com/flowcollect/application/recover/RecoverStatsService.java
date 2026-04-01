package com.flowcollect.application.recover;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.application.reminder.ReminderRuleService;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.reminder.RuleMode;
import com.flowcollect.infrastructure.persistence.invoice.FollowUpJpaRepository;
import com.flowcollect.infrastructure.persistence.invoice.PaymentJpaRepository;

@Service
public class RecoverStatsService {

    private final OrganizationService organizationService;
    private final ReminderRuleService reminderRuleService;
    private final PaymentJpaRepository paymentRepository;
    private final FollowUpJpaRepository followUpRepository;

    public RecoverStatsService(
            OrganizationService organizationService,
            ReminderRuleService reminderRuleService,
            PaymentJpaRepository paymentRepository,
            FollowUpJpaRepository followUpRepository) {
        this.organizationService = organizationService;
        this.reminderRuleService = reminderRuleService;
        this.paymentRepository = paymentRepository;
        this.followUpRepository = followUpRepository;
    }

    public RecoverStats getStats(UUID organizationId) {
        Organization org = organizationService.getById(organizationId);

        BigDecimal totalRecovered = paymentRepository.sumRecoveredAmountByOrgId(organizationId);
        if (totalRecovered == null) totalRecovered = BigDecimal.ZERO;

        long activeRules = reminderRuleService.getActiveAutoRulesForOrg(organizationId)
                .stream()
                .filter(r -> r.getMode() == RuleMode.AUTO)
                .count();

        // Day boundaries in org timezone, converted to UTC for DB comparison
        LocalDate today = LocalDate.now(org.getTimezone());
        Instant dayStart = today.atStartOfDay(org.getTimezone()).toInstant();
        Instant dayEnd   = today.plusDays(1).atStartOfDay(org.getTimezone()).toInstant();

        long sentToday    = followUpRepository.countSentAutoToday(organizationId, dayStart, dayEnd);
        long pendingToday = followUpRepository.countPendingAuto(organizationId);

        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Instant weekStartInstant = weekStart.atStartOfDay(org.getTimezone()).toInstant();
        Instant weekEndInstant   = weekStart.plusWeeks(1).atStartOfDay(org.getTimezone()).toInstant();
        long sentThisWeek = followUpRepository.countSentAutoThisWeek(organizationId, weekStartInstant, weekEndInstant);

        return new RecoverStats(totalRecovered, activeRules, sentToday, pendingToday, sentThisWeek);
    }

    public record RecoverStats(
            BigDecimal totalRecovered,
            long activeRules,
            long sentToday,
            long pendingToday,
            long sentThisWeek
    ) {}
}
