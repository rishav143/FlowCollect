package com.flowcollect.api.v1.confirmation.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Payload sent by a customer when self-reporting a payment via the public confirmation link.
 */
public class CustomerPaymentSubmitRequest {

    @NotNull(message = "Amount claimed is required")
    @DecimalMin(value = "0.01", message = "Amount claimed must be greater than zero")
    private BigDecimal amountClaimed;

    /** Optional free-text note, e.g. "Paid via NEFT, ref: TXN123456". Max 500 chars. */
    private String customerNote;

    public BigDecimal getAmountClaimed() { return amountClaimed; }
    public void setAmountClaimed(BigDecimal amountClaimed) { this.amountClaimed = amountClaimed; }

    public String getCustomerNote() { return customerNote; }
    public void setCustomerNote(String customerNote) { this.customerNote = customerNote; }
}
