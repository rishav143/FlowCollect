package com.flowcollect.api.v1.recover.dto;

import com.flowcollect.domain.invoice.followup.FollowUpChannel;

import java.util.UUID;

public class QueueItemResponse {

    private final UUID followUpId;
    private final UUID invoiceId;
    private final String invoiceNumber;
    private final String customerName;
    private final FollowUpChannel channel;
    private final int daysOverdue;

    public QueueItemResponse(UUID followUpId, UUID invoiceId, String invoiceNumber,
                             String customerName, FollowUpChannel channel, int daysOverdue) {
        this.followUpId    = followUpId;
        this.invoiceId     = invoiceId;
        this.invoiceNumber = invoiceNumber;
        this.customerName  = customerName;
        this.channel       = channel;
        this.daysOverdue   = daysOverdue;
    }

    public UUID getFollowUpId()    { return followUpId; }
    public UUID getInvoiceId()     { return invoiceId; }
    public String getInvoiceNumber(){ return invoiceNumber; }
    public String getCustomerName(){ return customerName; }
    public FollowUpChannel getChannel() { return channel; }
    public int getDaysOverdue()    { return daysOverdue; }
}
