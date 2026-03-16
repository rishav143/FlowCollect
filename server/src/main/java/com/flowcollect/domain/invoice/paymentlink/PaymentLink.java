package com.flowcollect.domain.invoice.paymentlink;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.flowcollect.domain.invoice.Invoice;

/**
 * Represents a tokenised, shareable payment link tied to an invoice.
 *
 * A single PaymentLink can be referenced by multiple FollowUp records
 * (one per channel) so the same link is embedded in SMS, WhatsApp, and email.
 *
 * Lifecycle:
 *   ACTIVE → (webhook) → PARTIALLY_PAID or PAID
 *   ACTIVE / PARTIALLY_PAID → (scheduler) → EXPIRED
 */
@Entity
@Table(
        name = "payment_links",
        indexes = {
                @Index(name = "idx_payment_link_token", columnList = "token", unique = true),
                @Index(name = "idx_payment_link_gateway_session", columnList = "gateway_session_id")
        }
)
public class PaymentLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relationships

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    // Core Attributes

    /** Publicly shareable token embedded in the redirect URL. */
    @NotNull
    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentGateway gateway;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentLinkStatus status;

    /** Invoice total amount at the time this link was created. */
    @NotNull
    @Column(name = "amount_due", nullable = false, precision = 15, scale = 2)
    private BigDecimal amountDue;

    /** ISO 4217 currency code (e.g. USD, INR). */
    @NotNull
    @Column(nullable = false, length = 3)
    private String currency;

    /** Gateway-specific session / link ID used to match incoming webhooks. */
    @Column(name = "gateway_session_id", length = 255)
    private String gatewaySessionId;

    /**
     * Direct URL on the payment gateway side (Stripe Checkout URL, Razorpay short URL).
     * The redirect endpoint forwards customers here after token lookup.
     */
    @Column(name = "gateway_payment_url", length = 1000)
    private String gatewayPaymentUrl;

    /**
     * Our stable public URL ({appBaseUrl}/pay/{token}).
     * This is the URL embedded in invoice messages; it never changes even
     * if the gateway session is recreated.
     */
    @NotNull
    @Column(name = "public_url", nullable = false, length = 500)
    private String publicUrl;

    // Timing

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // JPA

    public PaymentLink() {
        // JPA only
    }

    // Callbacks

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.status == null) {
            this.status = PaymentLinkStatus.ACTIVE;
        }
    }

    // Domain Behaviour

    /** Called when a gateway webhook confirms full payment. */
    public void markPaid() {
        this.status = PaymentLinkStatus.PAID;
        this.paidAt = Instant.now();
    }

    /** Called when a gateway webhook confirms partial payment. */
    public void markPartiallyPaid() {
        this.status = PaymentLinkStatus.PARTIALLY_PAID;
    }

    /** Expires the link if it has not been fully paid yet. */
    public void expire() {
        if (this.status != PaymentLinkStatus.PAID) {
            this.status = PaymentLinkStatus.EXPIRED;
        }
    }

    public void cancel() {
        if (this.status != PaymentLinkStatus.PAID) {
            this.status = PaymentLinkStatus.CANCELLED;
        }
    }

    /** Returns true when the link can still accept payments. */
    public boolean isActive() {
        return this.status == PaymentLinkStatus.ACTIVE
                || this.status == PaymentLinkStatus.PARTIALLY_PAID;
    }

    // Getters & Setters

    public UUID getId() { return id; }

    public Invoice getInvoice() { return invoice; }
    public void setInvoice(Invoice invoice) { this.invoice = invoice; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public PaymentGateway getGateway() { return gateway; }
    public void setGateway(PaymentGateway gateway) { this.gateway = gateway; }

    public PaymentLinkStatus getStatus() { return status; }
    public void setStatus(PaymentLinkStatus status) { this.status = status; }

    public BigDecimal getAmountDue() { return amountDue; }
    public void setAmountDue(BigDecimal amountDue) { this.amountDue = amountDue; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getGatewaySessionId() { return gatewaySessionId; }
    public void setGatewaySessionId(String gatewaySessionId) { this.gatewaySessionId = gatewaySessionId; }

    public String getGatewayPaymentUrl() { return gatewayPaymentUrl; }
    public void setGatewayPaymentUrl(String gatewayPaymentUrl) { this.gatewayPaymentUrl = gatewayPaymentUrl; }

    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public Instant getPaidAt() { return paidAt; }

    public Instant getCreatedAt() { return createdAt; }
}
