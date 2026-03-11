package com.paidpeace.api.v1.invoice.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.paidpeace.domain.invoice.followup.FollowUpChannel;
import com.paidpeace.domain.invoice.followup.FollowUpStatus;
import com.paidpeace.domain.invoice.followup.FollowUpTriggerType;

public class FollowUpResponse {
    private UUID id;
    private UUID invoiceId;
    private FollowUpChannel channel;
    private FollowUpTriggerType triggerType;
    private FollowUpStatus status;
    private UUID templateId;
    private UUID reminderRuleId;
    private LocalDate scheduledForDate;
    private Instant sentAt;
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(UUID invoiceId) {
        this.invoiceId = invoiceId;
    }

    public FollowUpChannel getChannel() {
        return channel;
    }

    public void setChannel(FollowUpChannel channel) {
        this.channel = channel;
    }

    public FollowUpTriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(FollowUpTriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public FollowUpStatus getStatus() {
        return status;
    }

    public void setStatus(FollowUpStatus status) {
        this.status = status;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public UUID getReminderRuleId() {
        return reminderRuleId;
    }

    public void setReminderRuleId(UUID reminderRuleId) {
        this.reminderRuleId = reminderRuleId;
    }

    public LocalDate getScheduledForDate() {
        return scheduledForDate;
    }

    public void setScheduledForDate(LocalDate scheduledForDate) {
        this.scheduledForDate = scheduledForDate;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

