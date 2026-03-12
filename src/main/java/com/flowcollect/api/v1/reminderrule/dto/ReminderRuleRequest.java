package com.flowcollect.api.v1.reminderrule.dto;

import java.util.UUID;

import com.flowcollect.domain.reminder.ReminderChannel;
import com.flowcollect.domain.reminder.ReminderTriggerType;
import com.flowcollect.domain.template.Template;

public class ReminderRuleRequest {
    private String name;
    private UUID organizationId;
    private int daysOffset;
    private ReminderTriggerType triggerType;
    private ReminderChannel channel;
    private Template template;
    private boolean active;

    public String getName() {
        return name;
    }

    public UUID getOrganizationId() {
        return organizationId;
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

    public void setName(String name) {
        this.name = name;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
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
}
