package com.flowcollect.api.v1.invoice.dto;

import com.flowcollect.domain.invoice.followup.FollowUpChannel;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for sending one consolidated message per channel to a customer
 * covering all their outstanding invoices.
 *
 * <p>One FollowUp record is created per invoice per channel for audit purposes,
 * but only a single message (with all PDFs attached if requested) is delivered.
 */
public class ConsolidatedFollowUpRequest {

    /** All invoice IDs to include — must belong to the same customer and organization. */
    @NotEmpty(message = "At least one invoice ID must be provided")
    private List<UUID> invoiceIds;

    /** Channels to send through. One message is sent per channel. */
    @NotEmpty(message = "At least one channel must be provided")
    private List<FollowUpChannel> channels;

    /** Optional template. When null, the built-in consolidated template for the channel is used. */
    private UUID templateId;

    public List<UUID> getInvoiceIds() { return invoiceIds; }
    public void setInvoiceIds(List<UUID> invoiceIds) { this.invoiceIds = invoiceIds; }

    public List<FollowUpChannel> getChannels() { return channels; }
    public void setChannels(List<FollowUpChannel> channels) { this.channels = channels; }

    public UUID getTemplateId() { return templateId; }
    public void setTemplateId(UUID templateId) { this.templateId = templateId; }
}
