package com.paidpeace.api.v1.invoice.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.paidpeace.domain.invoice.followup.FollowUpChannel;

import jakarta.validation.constraints.NotEmpty;

/**
 * DTO for creating and dispatching manual follow-ups across multiple channels.
 * Each channel results in a separate FollowUp instance.
 */
public class MultiChannelFollowUpRequest {

    @NotEmpty
    private List<FollowUpChannel> channels;

    private UUID templateId;

    private LocalDate scheduledForDate;

    private Boolean attachPdf;

    public List<FollowUpChannel> getChannels() {
        return channels;
    }

    public void setChannels(List<FollowUpChannel> channels) {
        this.channels = channels;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public void setTemplateId(UUID templateId) {
        this.templateId = templateId;
    }

    public LocalDate getScheduledForDate() {
        return scheduledForDate;
    }

    public void setScheduledForDate(LocalDate scheduledForDate) {
        this.scheduledForDate = scheduledForDate;
    }

    public Boolean getAttachPdf() {
        return attachPdf;
    }

    public void setAttachPdf(Boolean attachPdf) {
        this.attachPdf = attachPdf;
    }
}

