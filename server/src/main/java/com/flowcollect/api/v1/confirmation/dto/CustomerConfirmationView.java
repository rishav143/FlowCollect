package com.flowcollect.api.v1.confirmation.dto;

import com.flowcollect.api.v1.organization.dto.OrgPaymentDetailsResponse;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Read-only view of an invoice returned to the customer on the public confirmation page.
 * Contains only the information the customer needs to verify they are paying the right invoice.
 * Internal IDs are intentionally omitted.
 *
 * {@code paymentDetails} is included when the organisation has saved payment receiving
 * details (bank account, UPI, etc.) so the customer knows exactly where to send funds.
 */
public class CustomerConfirmationView {

    private String invoiceNumber;
    private String organizationName;
    private String customerName;
    private BigDecimal totalAmount;
    private BigDecimal totalPaid;
    private BigDecimal remainingAmount;
    private LocalDate dueDate;
    private String currency;
    /** OPEN or CLOSED — tells the customer whether the invoice can still accept submissions. */
    private String linkStatus;
    /** Optional — null when the organisation has not configured payment details. */
    private OrgPaymentDetailsResponse paymentDetails;

    public String getInvoiceNumber()                         { return invoiceNumber; }
    public String getOrganizationName()                      { return organizationName; }
    public String getCustomerName()                          { return customerName; }
    public BigDecimal getTotalAmount()                       { return totalAmount; }
    public BigDecimal getTotalPaid()                         { return totalPaid; }
    public BigDecimal getRemainingAmount()                   { return remainingAmount; }
    public LocalDate getDueDate()                            { return dueDate; }
    public String getCurrency()                              { return currency; }
    public String getLinkStatus()                            { return linkStatus; }
    public OrgPaymentDetailsResponse getPaymentDetails()     { return paymentDetails; }

    public void setInvoiceNumber(String invoiceNumber)                         { this.invoiceNumber = invoiceNumber; }
    public void setOrganizationName(String organizationName)                   { this.organizationName = organizationName; }
    public void setCustomerName(String customerName)                           { this.customerName = customerName; }
    public void setTotalAmount(BigDecimal totalAmount)                         { this.totalAmount = totalAmount; }
    public void setTotalPaid(BigDecimal totalPaid)                             { this.totalPaid = totalPaid; }
    public void setRemainingAmount(BigDecimal remaining)                       { this.remainingAmount = remaining; }
    public void setDueDate(LocalDate dueDate)                                  { this.dueDate = dueDate; }
    public void setCurrency(String currency)                                   { this.currency = currency; }
    public void setLinkStatus(String linkStatus)                               { this.linkStatus = linkStatus; }
    public void setPaymentDetails(OrgPaymentDetailsResponse paymentDetails)    { this.paymentDetails = paymentDetails; }
}
