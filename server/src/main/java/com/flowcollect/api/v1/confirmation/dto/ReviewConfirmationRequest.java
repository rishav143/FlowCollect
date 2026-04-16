package com.flowcollect.api.v1.confirmation.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Payload sent by a business user when approving or rejecting a payment confirmation.
 * The note is optional in both cases.
 * notifyCustomer defaults to true — the customer is notified unless explicitly opted out.
 * newDueDate is only used for the request-remaining action — if provided, the invoice
 * due date is updated before the installment request email is sent.
 */
public class ReviewConfirmationRequest {

    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;

    private boolean notifyCustomer = true;

    @FutureOrPresent(message = "New due date must be today or in the future")
    private LocalDate newDueDate;

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public boolean isNotifyCustomer() { return notifyCustomer; }
    public void setNotifyCustomer(boolean notifyCustomer) { this.notifyCustomer = notifyCustomer; }

    public LocalDate getNewDueDate() { return newDueDate; }
    public void setNewDueDate(LocalDate newDueDate) { this.newDueDate = newDueDate; }
}
