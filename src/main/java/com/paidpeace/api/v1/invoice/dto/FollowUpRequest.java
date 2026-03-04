package com.paidpeace.api.v1.invoice.dto;

import java.util.UUID;

import com.paidpeace.domain.invoice.followup.FollowUpChannel;
import com.paidpeace.domain.invoice.followup.FollowUpTriggerType;

/**
 * DTO for creating follow-ups.
 */
public class FollowUpRequest {

    private FollowUpChannel channel;

    private FollowUpTriggerType triggerType;

    private UUID invoiceId;

    private UUID templateId;

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

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }
}

