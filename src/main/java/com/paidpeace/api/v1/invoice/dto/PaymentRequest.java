package com.paidpeace.api.v1.invoice.dto;

import java.math.BigDecimal;

import com.paidpeace.domain.invoice.payment.PaymentMode;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class PaymentRequest {

    @Positive(message = "Amount must be greater than 0")
    private BigDecimal amount;

    private PaymentMode mode;

    @Size(max = 100, message = "Reference ID must be less than 100 characters")
    private String referenceId;
    
    @Size(max = 255, message = "Notes must be less than 255 characters")
    private String notes;

    /* ======================
       Getters & Setters
       ====================== */

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMode getMode() {
        return mode;
    }

    public void setMode(PaymentMode mode) {
        this.mode = mode;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
