package com.flowcollect.domain.confirmation;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Records a customer's self-reported payment claim against an invoice.
 *
 * <p>Created when a customer visits the confirmation link page and submits
 * their payment details. Multiple confirmations can exist per link (if the
 * business rejects a submission, the customer may resubmit).
 *
 * <p>Only one {@link PaymentConfirmationStatus#PENDING_APPROVAL} confirmation
 * is allowed per link at a time — enforced at the service layer.
 *
 * <p>On approval, the service layer records the {@link #amountClaimed} as a
 * payment on the invoice and closes the {@link ConfirmationLink} if the invoice
 * becomes fully paid.
 */
@Entity
@Table(name = "payment_confirmations")
public class PaymentConfirmation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "confirmation_link_id", nullable = false)
    private ConfirmationLink confirmationLink;

    /**
     * Amount the customer claims to have paid.
     * Must be > 0 and ≤ the invoice's remaining amount (validated at service layer).
     */
    @NotNull
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amountClaimed;

    /**
     * Optional free-text note from the customer, e.g. "Paid via NEFT, ref: TXN123456".
     */
    @Column(length = 500)
    private String customerNote;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentConfirmationStatus status = PaymentConfirmationStatus.PENDING_APPROVAL;

    /**
     * Optional note added by the business user when approving or rejecting.
     */
    @Column(length = 500)
    private String businessNote;

    /** Timestamp of the business user's approve/reject action. */
    private Instant reviewedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // JPA
    public PaymentConfirmation() {}

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Domain behavior

    /**
     * Approves this confirmation. Transitions status to {@link PaymentConfirmationStatus#APPROVED}.
     *
     * @param businessNote optional note from the business user (may be null)
     * @throws IllegalStateException if not currently in PENDING_APPROVAL state
     */
    public void approve(String businessNote) {
        if (this.status != PaymentConfirmationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                "Cannot approve confirmation " + this.id + " — current status: " + this.status);
        }
        this.status = PaymentConfirmationStatus.APPROVED;
        this.businessNote = businessNote;
        this.reviewedAt = Instant.now();
    }

    /**
     * Rejects this confirmation. Transitions status to {@link PaymentConfirmationStatus#REJECTED}.
     * The confirmation link remains OPEN so the customer may resubmit.
     *
     * @param businessNote optional note from the business user (may be null)
     * @throws IllegalStateException if not currently in PENDING_APPROVAL state
     */
    public void reject(String businessNote) {
        if (this.status != PaymentConfirmationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException(
                "Cannot reject confirmation " + this.id + " — current status: " + this.status);
        }
        this.status = PaymentConfirmationStatus.REJECTED;
        this.businessNote = businessNote;
        this.reviewedAt = Instant.now();
    }

    public boolean isPendingApproval() { return this.status == PaymentConfirmationStatus.PENDING_APPROVAL; }
    public boolean isApproved()        { return this.status == PaymentConfirmationStatus.APPROVED; }
    public boolean isRejected()        { return this.status == PaymentConfirmationStatus.REJECTED; }

    // Getters

    public UUID getId()                             { return id; }
    public ConfirmationLink getConfirmationLink()   { return confirmationLink; }
    public BigDecimal getAmountClaimed()            { return amountClaimed; }
    public String getCustomerNote()                 { return customerNote; }
    public PaymentConfirmationStatus getStatus()    { return status; }
    public String getBusinessNote()                 { return businessNote; }
    public Instant getReviewedAt()                  { return reviewedAt; }
    public Instant getCreatedAt()                   { return createdAt; }

    // Setters

    public void setConfirmationLink(ConfirmationLink confirmationLink) { this.confirmationLink = confirmationLink; }
    public void setAmountClaimed(BigDecimal amountClaimed)             { this.amountClaimed = amountClaimed; }
    public void setCustomerNote(String customerNote)                   { this.customerNote = customerNote; }
}
