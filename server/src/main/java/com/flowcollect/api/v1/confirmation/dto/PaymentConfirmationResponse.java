package com.flowcollect.api.v1.confirmation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.flowcollect.domain.confirmation.PaymentConfirmationStatus;

/**
 * Full view of a payment confirmation returned to the authenticated business user.
 */
public class PaymentConfirmationResponse {

    private UUID id;
    private UUID invoiceId;
    private String invoiceNumber;
    private UUID confirmationLinkId;
    private BigDecimal amountClaimed;
    private String customerNote;
    private PaymentConfirmationStatus status;
    private String businessNote;
    private Instant reviewedAt;
    private Instant createdAt;

    public UUID getId()                              { return id; }
    public UUID getInvoiceId()                       { return invoiceId; }
    public String getInvoiceNumber()                 { return invoiceNumber; }
    public UUID getConfirmationLinkId()              { return confirmationLinkId; }
    public BigDecimal getAmountClaimed()             { return amountClaimed; }
    public String getCustomerNote()                  { return customerNote; }
    public PaymentConfirmationStatus getStatus()     { return status; }
    public String getBusinessNote()                  { return businessNote; }
    public Instant getReviewedAt()                   { return reviewedAt; }
    public Instant getCreatedAt()                    { return createdAt; }

    public void setId(UUID id)                                         { this.id = id; }
    public void setInvoiceId(UUID invoiceId)                           { this.invoiceId = invoiceId; }
    public void setInvoiceNumber(String invoiceNumber)                 { this.invoiceNumber = invoiceNumber; }
    public void setConfirmationLinkId(UUID confirmationLinkId)         { this.confirmationLinkId = confirmationLinkId; }
    public void setAmountClaimed(BigDecimal amountClaimed)             { this.amountClaimed = amountClaimed; }
    public void setCustomerNote(String customerNote)                   { this.customerNote = customerNote; }
    public void setStatus(PaymentConfirmationStatus status)            { this.status = status; }
    public void setBusinessNote(String businessNote)                   { this.businessNote = businessNote; }
    public void setReviewedAt(Instant reviewedAt)                      { this.reviewedAt = reviewedAt; }
    public void setCreatedAt(Instant createdAt)                        { this.createdAt = createdAt; }
}
