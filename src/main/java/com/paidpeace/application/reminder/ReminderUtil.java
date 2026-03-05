package com.paidpeace.application.reminder;

import java.util.UUID;

import com.paidpeace.domain.reminder.ReminderRule;
import com.paidpeace.infrastructure.persistence.reminder.ReminderRuleJpaRepository;
import com.paidpeace.exception.http.NotFoundException;
import com.paidpeace.exception.code.ReminderRuleErrorCode;

public class ReminderUtil {
    public static ReminderRule validateReminderRule
    (
        UUID reminderRuleId,
        ReminderRuleJpaRepository reminderRuleRepository
    ) {
        if(reminderRuleId == null) {
            throw new NotFoundException(ReminderRuleErrorCode.REMINDER_NOT_FOUND, 
                "Reminder rule ID cannot be null");
        }
        ReminderRule reminderRule = reminderRuleRepository.findById(reminderRuleId)
            .orElseThrow(() -> new NotFoundException(ReminderRuleErrorCode.REMINDER_NOT_FOUND, 
                "Reminder rule not found"));

        return reminderRule;
    }

    public static ReminderRule validateReminderRuleAndOrganization
    (
        UUID reminderRuleId,
        UUID organizationId,
        ReminderRuleJpaRepository reminderRuleRepository
    ) {
        if(organizationId == null) {
            throw new NotFoundException(ReminderRuleErrorCode.REMINDER_NOT_FOUND, 
                "Organization ID cannot be null");
        }
        ReminderRule reminderRule = validateReminderRule(reminderRuleId, reminderRuleRepository);
        if(reminderRule.getOrganization().getId() != organizationId) {
            throw new NotFoundException(ReminderRuleErrorCode.REMINDER_NOT_FOUND, 
                "Reminder rule not found");
        }
        return reminderRule;
    }
}
