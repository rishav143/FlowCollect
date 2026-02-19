package com.cashclarity.api.v1.invoice.dto;

import com.cashclarity.domain.invoice.followup.FollowUpChannel;
import com.cashclarity.domain.invoice.followup.FollowUpTriggerType;
import java.util.UUID;

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

