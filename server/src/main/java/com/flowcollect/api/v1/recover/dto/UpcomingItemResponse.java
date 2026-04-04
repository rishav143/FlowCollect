package com.flowcollect.api.v1.recover.dto;

import com.flowcollect.domain.reminder.ReminderChannel;

import java.time.LocalDate;
import java.util.UUID;

public class UpcomingItemResponse {

    private final UUID invoiceId;
    private final String invoiceNumber;
    private final String customerName;
    private final ReminderChannel channel;
    private final LocalDate scheduledDate;
    private final String ruleName;
    private final int daysUntil;

    public UpcomingItemResponse(UUID invoiceId, String invoiceNumber, String customerName,
                                ReminderChannel channel, LocalDate scheduledDate,
                                String ruleName, int daysUntil) {
        this.invoiceId     = invoiceId;
        this.invoiceNumber = invoiceNumber;
        this.customerName  = customerName;
        this.channel       = channel;
        this.scheduledDate = scheduledDate;
        this.ruleName      = ruleName;
        this.daysUntil     = daysUntil;
    }

    public UUID getInvoiceId()           { return invoiceId; }
    public String getInvoiceNumber()     { return invoiceNumber; }
    public String getCustomerName()      { return customerName; }
    public ReminderChannel getChannel()  { return channel; }
    public LocalDate getScheduledDate()  { return scheduledDate; }
    public String getRuleName()          { return ruleName; }
    public int getDaysUntil()            { return daysUntil; }
}
