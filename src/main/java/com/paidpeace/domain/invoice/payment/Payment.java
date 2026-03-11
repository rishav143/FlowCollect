package com.paidpeace.domain.invoice.payment;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

import com.paidpeace.domain.invoice.Invoice;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relationships

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    // Core Attributes

    @Positive
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMode mode;

    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(length = 255)
    private String notes;

    // Audit

    @Column(nullable = false, updatable = false)
    private Instant paidAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // JPA

    public Payment() {
        // JPA only
    }

    // Callbacks

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.paidAt = this.createdAt;
    }

    // Getters & Setters

    public UUID getId() {
        return id;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        if(amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount cannot be null or negative");
        }
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    public PaymentMode getMode() {
        return mode;
    }

    public void setMode(PaymentMode mode) {
        this.mode = mode;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(Instant paidAt) {
        this.paidAt = paidAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
