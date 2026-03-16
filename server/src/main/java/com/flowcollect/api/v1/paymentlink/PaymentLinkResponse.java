package com.flowcollect.api.v1.paymentlink;

import com.flowcollect.domain.invoice.paymentlink.PaymentGateway;
import com.flowcollect.domain.invoice.paymentlink.PaymentLinkStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Read-only projection of a PaymentLink returned to API callers. */
public class PaymentLinkResponse {

    private UUID id;
    private UUID invoiceId;
    private PaymentGateway gateway;
    private PaymentLinkStatus status;
    private BigDecimal amountDue;
    private String currency;
    private String publicUrl;
    private Instant expiresAt;
    private Instant paidAt;
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getInvoiceId() { return invoiceId; }
    public void setInvoiceId(UUID invoiceId) { this.invoiceId = invoiceId; }

    public PaymentGateway getGateway() { return gateway; }
    public void setGateway(PaymentGateway gateway) { this.gateway = gateway; }

    public PaymentLinkStatus getStatus() { return status; }
    public void setStatus(PaymentLinkStatus status) { this.status = status; }

    public BigDecimal getAmountDue() { return amountDue; }
    public void setAmountDue(BigDecimal amountDue) { this.amountDue = amountDue; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static PaymentLinkResponse from(com.flowcollect.domain.invoice.paymentlink.PaymentLink link) {
        PaymentLinkResponse r = new PaymentLinkResponse();
        r.setId(link.getId());
        r.setInvoiceId(link.getInvoice() != null ? link.getInvoice().getId() : null);
        r.setGateway(link.getGateway());
        r.setStatus(link.getStatus());
        r.setAmountDue(link.getAmountDue());
        r.setCurrency(link.getCurrency());
        r.setPublicUrl(link.getPublicUrl());
        r.setExpiresAt(link.getExpiresAt());
        r.setPaidAt(link.getPaidAt());
        r.setCreatedAt(link.getCreatedAt());
        return r;
    }
}
