package com.flowcollect.application.reminder;

import java.util.UUID;

import com.flowcollect.domain.reminder.ReminderRule;
import com.flowcollect.infrastructure.persistence.reminder.ReminderRuleJpaRepository;
import com.flowcollect.exception.http.NotFoundException;

public class ReminderUtil {
    public static ReminderRule validateReminderRule
    (
        UUID reminderRuleId,
        ReminderRuleJpaRepository reminderRuleRepository
    ) {
        if(reminderRuleId == null) {
            throw new NotFoundException( 
                "Reminder rule ID cannot be null");
        }
        ReminderRule reminderRule = reminderRuleRepository.findById(reminderRuleId)
            .orElseThrow(() -> new NotFoundException( 
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
            throw new NotFoundException(
                "Organization ID cannot be null");
        }
        ReminderRule reminderRule = validateReminderRule(reminderRuleId, reminderRuleRepository);
        // System-defined rules are platform blueprints — block all mutations from org context
        if (reminderRule.isSystemDefined()) {
            throw new NotFoundException("Reminder rule not found");
        }
        if (reminderRule.getOrganization() == null ||
                !reminderRule.getOrganization().getId().equals(organizationId)) {
            throw new NotFoundException(
                "Reminder rule not found");
        }
        return reminderRule;
    }
}
