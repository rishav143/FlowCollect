package com.flowcollect.domain.invoice;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Entity
@Table(name = "invoice_items")
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relationships

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    // Core Attributes

    @NotBlank
    @Column(nullable = false)
    private String description;

    @NotNull
    @Positive
    @Column(nullable = false)
    private int quantity;

    @NotNull
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @NotNull
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    // JPA

    protected InvoiceItem() {
        // JPA only
    }

    // Domain Behavior

    // Creates a new invoice item with computed amount (quantity × unitPrice).
    public static InvoiceItem create(Invoice invoice, String description, int quantity, BigDecimal unitPrice) {
        if (invoice == null) {
            throw new IllegalArgumentException("Invoice cannot be null");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be null or blank");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be null or negative");
        }
        InvoiceItem item = new InvoiceItem();
        item.invoice = invoice;
        item.description = description.trim();
        item.quantity = quantity;
        item.unitPrice = unitPrice.setScale(0, RoundingMode.HALF_UP);
        item.amount = item.unitPrice.multiply(BigDecimal.valueOf(item.quantity)).setScale(0, RoundingMode.HALF_UP);
        return item;
    }

    // Recomputes amount from quantity and unitPrice. Use when quantity or unitPrice is changed.
    public void recalculateAmount() {
        this.amount = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(0, RoundingMode.HALF_UP);
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public String getDescription() {
        return description;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    // Setters

    public void setInvoice(Invoice invoice) {
        this.invoice = invoice;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.quantity = quantity;
        recalculateAmount();
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Unit price cannot be null or negative");
        }
        this.unitPrice = unitPrice.setScale(0, RoundingMode.HALF_UP);
        recalculateAmount();
    }

    // For JPA hydration only. Prefer create() or setters for quantity/unitPrice which auto-recalculate.
    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? amount.setScale(0, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }
}
