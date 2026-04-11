package com.flowcollect.api.v1.reminderrule.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.flowcollect.domain.reminder.ReminderChannel;
import com.flowcollect.domain.reminder.ReminderTriggerType;
import com.flowcollect.domain.reminder.RuleMode;

public class ReminderRuleResponse {
    private UUID id;
    private String name;
    private int daysOffset;
    private ReminderTriggerType triggerType;
    private ReminderChannel channel;
    private UUID templateId;
    private String templateName;
    private boolean attachPdf;
    private boolean active;
    private int maxOccurrences;
    private int cycleIntervalDays;
    private List<UUID> occurrenceTemplateIds;
    private List<String> occurrenceTemplateNames;
    private LocalDate startDate;
    private RuleMode mode;
    private boolean systemDefined;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getDaysOffset() { return daysOffset; }
    public void setDaysOffset(int daysOffset) { this.daysOffset = daysOffset; }

    public ReminderTriggerType getTriggerType() { return triggerType; }
    public void setTriggerType(ReminderTriggerType triggerType) { this.triggerType = triggerType; }

    public ReminderChannel getChannel() { return channel; }
    public void setChannel(ReminderChannel channel) { this.channel = channel; }

    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }

    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    public boolean isAttachPdf() { return attachPdf; }
    public void setAttachPdf(boolean attachPdf) { this.attachPdf = attachPdf; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getMaxOccurrences() { return maxOccurrences; }
    public void setMaxOccurrences(int maxOccurrences) { this.maxOccurrences = maxOccurrences; }

    public int getCycleIntervalDays() { return cycleIntervalDays; }
    public void setCycleIntervalDays(int cycleIntervalDays) { this.cycleIntervalDays = cycleIntervalDays; }

    public List<UUID> getOccurrenceTemplateIds() { return occurrenceTemplateIds; }
    public void setOccurrenceTemplateIds(List<UUID> occurrenceTemplateIds) { this.occurrenceTemplateIds = occurrenceTemplateIds; }

    public List<String> getOccurrenceTemplateNames() { return occurrenceTemplateNames; }
    public void setOccurrenceTemplateNames(List<String> occurrenceTemplateNames) { this.occurrenceTemplateNames = occurrenceTemplateNames; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public RuleMode getMode() { return mode; }
    public void setMode(RuleMode mode) { this.mode = mode; }

    public boolean isSystemDefined() { return systemDefined; }
    public void setSystemDefined(boolean systemDefined) { this.systemDefined = systemDefined; }
}
