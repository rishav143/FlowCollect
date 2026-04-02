package com.flowcollect.api.v1.recover.dto;

import com.flowcollect.domain.invoice.followup.FollowUpChannel;
import com.flowcollect.domain.invoice.followup.FollowUpStatus;

import java.time.Instant;
import java.util.UUID;

public class ActivityItemResponse {

    private final UUID followUpId;
    private final String invoiceNumber;
    private final String customerName;
    private final FollowUpChannel channel;
    private final FollowUpStatus status;
    private final String ruleName;
    private final Instant eventAt;   // sentAt for SENT, updatedAt for CANCELLED

    public ActivityItemResponse(UUID followUpId, String invoiceNumber, String customerName,
                                FollowUpChannel channel, FollowUpStatus status,
                                String ruleName, Instant eventAt) {
        this.followUpId    = followUpId;
        this.invoiceNumber = invoiceNumber;
        this.customerName  = customerName;
        this.channel       = channel;
        this.status        = status;
        this.ruleName      = ruleName;
        this.eventAt       = eventAt;
    }

    public UUID getFollowUpId()     { return followUpId; }
    public String getInvoiceNumber(){ return invoiceNumber; }
    public String getCustomerName() { return customerName; }
    public FollowUpChannel getChannel() { return channel; }
    public FollowUpStatus getStatus()   { return status; }
    public String getRuleName()     { return ruleName; }
    public Instant getEventAt()     { return eventAt; }
}
