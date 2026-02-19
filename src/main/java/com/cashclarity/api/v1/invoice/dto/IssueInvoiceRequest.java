package com.cashclarity.api.v1.invoice.dto;

import java.time.LocalDate;

/**
 * DTO for issuing an invoice. issueDate is optional; when omitted the invoice
 * will be issued with the current date.
 */
public class IssueInvoiceRequest {

    private LocalDate issueDate;

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }
}

