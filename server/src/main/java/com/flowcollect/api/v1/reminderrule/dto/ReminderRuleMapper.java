package com.flowcollect.api.v1.reminderrule.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.flowcollect.domain.reminder.ReminderRule;

public class ReminderRuleMapper {

    /**
     * Maps a rule to a response, resolving occurrence template names from the supplied map.
     *
     * @param rule     the rule to map
     * @param nameById map of template id → template name; used to populate occurrenceTemplateNames
     */
    public static ReminderRuleResponse toResponse(ReminderRule rule, Map<UUID, String> nameById) {
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
        List<UUID> occIds = rule.getOccurrenceTemplateIds();
        response.setOccurrenceTemplateIds(occIds);
        if (occIds != null && !occIds.isEmpty()) {
            response.setOccurrenceTemplateNames(
                occIds.stream()
                      .map(id -> nameById.getOrDefault(id, null))
                      .collect(Collectors.toList())
            );
        }
        response.setStartDate(rule.getStartDate());
        response.setMode(rule.getMode());
        response.setSystemDefined(rule.isSystemDefined());
        response.setCreatedAt(rule.getCreatedAt());
        response.setUpdatedAt(rule.getUpdatedAt());
        return response;
    }

    /** Convenience overload when no occurrence name resolution is needed. */
    public static ReminderRuleResponse toResponse(ReminderRule rule) {
        return toResponse(rule, Map.of());
    }
}
