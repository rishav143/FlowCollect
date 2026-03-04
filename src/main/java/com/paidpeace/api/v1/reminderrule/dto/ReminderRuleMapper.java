package com.paidpeace.api.v1.reminderrule.dto;

import com.paidpeace.domain.reminder.ReminderRule;

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
        response.setCreatedAt(reminderRule.getCreatedAt());
        response.setUpdatedAt(reminderRule.getUpdatedAt());
        return response;
    }
}
