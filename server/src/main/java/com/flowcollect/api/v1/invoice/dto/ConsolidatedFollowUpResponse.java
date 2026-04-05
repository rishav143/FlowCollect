package com.flowcollect.api.v1.invoice.dto;

import com.flowcollect.domain.invoice.followup.FollowUpChannel;
import com.flowcollect.domain.invoice.followup.FollowUpStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response for a consolidated dispatch — one entry per channel.
 * Each entry references the FollowUp records created per invoice,
 * but confirms only one external message was sent.
 */
public class ConsolidatedFollowUpResponse {

    private FollowUpChannel channel;
    private FollowUpStatus  status;
    private String          externalMessageId;   // Resend email ID, etc.
    private Instant         sentAt;
    private List<UUID>      followUpIds;         // One per invoice, for audit
    private List<UUID>      invoiceIds;
    private String          errorMessage;        // Non-null only when status=FAILED

    public FollowUpChannel getChannel() { return channel; }
    public void setChannel(FollowUpChannel channel) { this.channel = channel; }

    public FollowUpStatus getStatus() { return status; }
    public void setStatus(FollowUpStatus status) { this.status = status; }

    public String getExternalMessageId() { return externalMessageId; }
    public void setExternalMessageId(String externalMessageId) { this.externalMessageId = externalMessageId; }

    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }

    public List<UUID> getFollowUpIds() { return followUpIds; }
    public void setFollowUpIds(List<UUID> followUpIds) { this.followUpIds = followUpIds; }

    public List<UUID> getInvoiceIds() { return invoiceIds; }
    public void setInvoiceIds(List<UUID> invoiceIds) { this.invoiceIds = invoiceIds; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
