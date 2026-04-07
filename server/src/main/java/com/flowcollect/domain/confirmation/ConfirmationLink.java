package com.flowcollect.domain.confirmation;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

import com.flowcollect.domain.invoice.Invoice;

/**
 * A publicly shareable link that lets a customer self-report their payment for an invoice.
 *
 * <p>One link is created per invoice when the organization uses
 * {@link com.flowcollect.domain.organization.PaymentCollectionMode#CONFIRMATION_FLOW}.
 * The link is created lazily — on the first follow-up dispatch — to avoid creating
 * links for invoices that may never need them.
 *
 * <p>The link stays {@link ConfirmationLinkStatus#OPEN} until the invoice is fully paid
 * or cancelled, at which point it is {@link ConfirmationLinkStatus#CLOSED} by the service layer.
 */
@Entity
@Table(name = "confirmation_links")
public class ConfirmationLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The invoice this confirmation link belongs to.
     * Unique constraint enforces exactly one link per invoice.
     */
    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false, unique = true)
    private Invoice invoice;

    /**
     * Cryptographically random token embedded in the public URL.
     * 32 hex characters (128-bit UUID without dashes).
     */
    @NotNull
    @Column(nullable = false, unique = true, length = 32)
    private String token;

    /**
     * The full public URL sent to customers, e.g. {@code https://app.flowcollect.io/confirm/{token}}.
     */
    @NotNull
    @Column(nullable = false)
    private String publicUrl;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConfirmationLinkStatus status = ConfirmationLinkStatus.OPEN;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    // JPA
    public ConfirmationLink() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Domain behavior

    public boolean isOpen() {
        return this.status == ConfirmationLinkStatus.OPEN;
    }

    /**
     * Closes this link. Called when the invoice transitions to PAID or CANCELLED.
     * Idempotent: closing an already-closed link is a no-op.
     */
    public void close() {
        this.status = ConfirmationLinkStatus.CLOSED;
    }

    /**
     * Reopens this link. Called when a recorded payment is deleted or reduced and the
     * invoice reverts to an unpaid state, allowing the customer to resubmit.
     * Idempotent: reopening an already-open link is a no-op.
     */
    public void reopen() {
        this.status = ConfirmationLinkStatus.OPEN;
    }

    // Getters

    public UUID getId() { return id; }

    public Invoice getInvoice() { return invoice; }

    public String getToken() { return token; }

    public String getPublicUrl() { return publicUrl; }

    public ConfirmationLinkStatus getStatus() { return status; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }

    // Setters

    public void setInvoice(Invoice invoice) { this.invoice = invoice; }

    public void setToken(String token) { this.token = token; }

    public void setPublicUrl(String publicUrl) { this.publicUrl = publicUrl; }
}
