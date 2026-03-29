package com.flowcollect.api.v1.reminderrule.dto;

import com.flowcollect.domain.reminder.ReminderRule;

public class ReminderRuleMapper {
    public static ReminderRuleResponse toResponse(ReminderRule rule) {
        ReminderRuleResponse response = new ReminderRuleResponse();
        response.setId(rule.getId());
        response.setName(rule.getName());
        response.setDaysOffset(rule.getDaysOffset());
        response.setTriggerType(rule.getTriggerType());
        response.setChannel(rule.getChannel());
        if (rule.getTemplate() != null) {
            response.setTemplateId(rule.getTemplate().getId());
            response.setTemplateName(rule.getTemplate().getName());
        }
        response.setAttachPdf(rule.isAttachPdf());
        response.setActive(rule.isActive());
        response.setMaxOccurrences(rule.getMaxOccurrences());
        response.setCycleIntervalDays(rule.getCycleIntervalDays());
        response.setOccurrenceTemplateIds(rule.getOccurrenceTemplateIds());
        response.setStartDate(rule.getStartDate());
        response.setCreatedAt(rule.getCreatedAt());
        response.setUpdatedAt(rule.getUpdatedAt());
        return response;
    }
}
