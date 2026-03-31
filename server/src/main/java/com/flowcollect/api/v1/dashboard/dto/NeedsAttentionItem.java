package com.flowcollect.api.v1.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class NeedsAttentionItem {

    private UUID invoiceId;
    private String invoiceNumber;
    private UUID customerId;
    private String customerName;
    private LocalDate dueDate;
    private BigDecimal remainingAmount;
    private int daysPastDue;

    public NeedsAttentionItem(
            UUID invoiceId,
            String invoiceNumber,
            UUID customerId,
            String customerName,
            LocalDate dueDate,
            BigDecimal remainingAmount,
            int daysPastDue
    ) {
        this.invoiceId = invoiceId;
        this.invoiceNumber = invoiceNumber;
        this.customerId = customerId;
        this.customerName = customerName;
        this.dueDate = dueDate;
        this.remainingAmount = remainingAmount;
        this.daysPastDue = daysPastDue;
    }

    public UUID getInvoiceId() { return invoiceId; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public UUID getCustomerId() { return customerId; }
    public String getCustomerName() { return customerName; }
    public LocalDate getDueDate() { return dueDate; }
    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public int getDaysPastDue() { return daysPastDue; }
}
