package com.paidpeace.api.v1.reminderrule.dto;

import java.time.Instant;
import java.util.UUID;

import com.paidpeace.domain.reminder.ReminderChannel;
import com.paidpeace.domain.reminder.ReminderTriggerType;
import com.paidpeace.domain.template.Template;

public class ReminderRuleResponse {
    private UUID id;
    private String name;
    private int daysOffset;
    private ReminderTriggerType triggerType;
    private ReminderChannel channel;
    private Template template;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
    
    public int getDaysOffset() {
        return daysOffset;
    }

    public ReminderTriggerType getTriggerType() {
        return triggerType;
    }

    public ReminderChannel getChannel() {
        return channel;
    }

    public Template getTemplate() {
        return template;
    }

    public boolean isActive() {
        return active;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public void setDaysOffset(int daysOffset) {
        this.daysOffset = daysOffset;
    }

    public void setTriggerType(ReminderTriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public void setChannel(ReminderChannel channel) {
        this.channel = channel;
    }
    
    public void setTemplate(Template template) {
        this.template = template;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
