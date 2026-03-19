package com.flowcollect.api.v1.confirmation.dto;

import jakarta.validation.constraints.Size;

/**
 * Payload sent by a business user when approving or rejecting a payment confirmation.
 * The note is optional in both cases.
 * notifyCustomer defaults to true — the customer is notified unless explicitly opted out.
 */
public class ReviewConfirmationRequest {

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;

    private boolean notifyCustomer = true;

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public boolean isNotifyCustomer() { return notifyCustomer; }
    public void setNotifyCustomer(boolean notifyCustomer) { this.notifyCustomer = notifyCustomer; }
}
