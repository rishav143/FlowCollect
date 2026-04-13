package com.flowcollect.api.v1.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
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

    @Valid
    private List<TaxLineRequest> taxLines = new ArrayList<>();

    @DecimalMin(value = "0.00", message = "discountAmount must be >= 0")
    private BigDecimal discountAmount;

    @Valid
    private List<InvoiceItemRequest> items = new ArrayList<>();

    public UUID getCreatedByUserId()                          { return createdByUserId; }
    public void setCreatedByUserId(UUID createdByUserId)      { this.createdByUserId = createdByUserId; }
    public UUID getCustomerId()                               { return customerId; }
    public void setCustomerId(UUID customerId)                { this.customerId = customerId; }
    public String getInvoiceNumber()                          { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber)        { this.invoiceNumber = invoiceNumber; }
    public LocalDate getIssueDate()                           { return issueDate; }
    public void setIssueDate(LocalDate issueDate)             { this.issueDate = issueDate; }
    public LocalDate getDueDate()                             { return dueDate; }
    public void setDueDate(LocalDate dueDate)                 { this.dueDate = dueDate; }
    public List<TaxLineRequest> getTaxLines()                 { return taxLines; }
    public void setTaxLines(List<TaxLineRequest> taxLines)    { this.taxLines = taxLines; }
    public BigDecimal getDiscountAmount()                     { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount)  { this.discountAmount = discountAmount; }
    public List<InvoiceItemRequest> getItems()                { return items; }
    public void setItems(List<InvoiceItemRequest> items)      { this.items = items; }
}
