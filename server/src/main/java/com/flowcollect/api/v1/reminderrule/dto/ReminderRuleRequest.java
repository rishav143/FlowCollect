package com.flowcollect.api.v1.reminderrule.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.flowcollect.domain.reminder.ReminderChannel;
import com.flowcollect.domain.reminder.ReminderTriggerType;

public class ReminderRuleRequest {
    private String name;
    private UUID organizationId;
    private Integer daysOffset;
    private ReminderTriggerType triggerType;
    private ReminderChannel channel;
    private UUID templateId;
    private Boolean active;

    // Cycle & Recurrence
    // Nullable — null means "not provided" (use existing value on update, default on create).
    private Integer maxOccurrences;
    private Integer cycleIntervalDays;
    private LocalDate startDate;

    public String getName() {
        return name;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public Integer getDaysOffset() {
        return daysOffset;
    }

    public ReminderTriggerType getTriggerType() {
        return triggerType;
    }

    public ReminderChannel getChannel() {
        return channel;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public Boolean isActive() {
        return active;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public void setDaysOffset(Integer daysOffset) {
        this.daysOffset = daysOffset;
    }

    public void setTriggerType(ReminderTriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public void setChannel(ReminderChannel channel) {
        this.channel = channel;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Integer getMaxOccurrences() {
        return maxOccurrences;
    }

    public void setMaxOccurrences(Integer maxOccurrences) {
        this.maxOccurrences = maxOccurrences;
    }

    public Integer getCycleIntervalDays() {
        return cycleIntervalDays;
    }

    public void setCycleIntervalDays(Integer cycleIntervalDays) {
        this.cycleIntervalDays = cycleIntervalDays;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
}
