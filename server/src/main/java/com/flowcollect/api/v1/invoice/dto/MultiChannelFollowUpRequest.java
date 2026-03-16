package com.flowcollect.api.v1.invoice.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.flowcollect.domain.invoice.followup.FollowUpChannel;
import com.flowcollect.domain.invoice.paymentlink.PaymentGateway;

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

    /**
     * When true, a payment link is generated and embedded in the message body
     * via the {{paymentLink}} template placeholder.
     * Requires paymentGateway to be specified.
     */
    private boolean includePaymentLink = false;

    /**
     * Gateway to use for generating the payment link.
     * Required when includePaymentLink is true.
     */
    private PaymentGateway paymentGateway;

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

    public boolean isIncludePaymentLink() {
        return includePaymentLink;
    }

    public void setIncludePaymentLink(boolean includePaymentLink) {
        this.includePaymentLink = includePaymentLink;
    }

    public PaymentGateway getPaymentGateway() {
        return paymentGateway;
    }

    public void setPaymentGateway(PaymentGateway paymentGateway) {
        this.paymentGateway = paymentGateway;
    }
}

