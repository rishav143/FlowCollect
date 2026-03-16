package com.flowcollect.api.v1.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InvoiceRequest {

    private UUID createdByUserId;

    private UUID customerId;

    @NotBlank(message = "invoiceNumber must not be blank")
    @Size(max = 100, message = "invoiceNumber must not exceed 100 characters")
    private String invoiceNumber;

    private LocalDate issueDate;

    private LocalDate dueDate;

    @DecimalMin(value = "0.00", message = "taxPercentage must be at least 0")
    @DecimalMax(value = "100.00", message = "taxPercentage must be at most 100")
    private BigDecimal taxPercentage;

    @Valid
    private List<InvoiceItemRequest> items = new ArrayList<>();

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(UUID createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public UUID getCustomerId() { return customerId; }

    public void setCustomerId(UUID customerId) { this.customerId = customerId; }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getTaxPercentage() {
        return taxPercentage;
    }

    public void setTaxPercentage(BigDecimal taxPercentage) {
        this.taxPercentage = taxPercentage;
    }

    public List<InvoiceItemRequest> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItemRequest> items) {
        this.items = items;
    }
}
