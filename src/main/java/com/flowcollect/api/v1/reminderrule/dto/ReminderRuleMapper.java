package com.flowcollect.api.v1.reminderrule.dto;

import com.flowcollect.domain.reminder.ReminderRule;

public class ReminderRuleMapper {
    public static ReminderRuleResponse toResponse(ReminderRule reminderRule) {
        ReminderRuleResponse response = new ReminderRuleResponse();
        response.setId(reminderRule.getId());
        response.setName(reminderRule.getName());
        response.setDaysOffset(reminderRule.getDaysOffset());
        response.setTriggerType(reminderRule.getTriggerType());
        response.setChannel(reminderRule.getChannel());
        response.setTemplate(reminderRule.getTemplate());
        response.setActive(reminderRule.isActive());
        response.setMaxOccurrences(reminderRule.getMaxOccurrences());
        response.setCycleIntervalDays(reminderRule.getCycleIntervalDays());
        response.setStartDate(reminderRule.getStartDate());
        response.setCreatedAt(reminderRule.getCreatedAt());
        response.setUpdatedAt(reminderRule.getUpdatedAt());
        return response;
    }
}
