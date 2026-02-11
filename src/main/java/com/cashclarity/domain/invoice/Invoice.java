package com.cashclarity.domain.invoice;

import com.cashclarity.domain.customer.Customer;
import com.cashclarity.domain.organization.Organization;
import com.cashclarity.domain.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(
        name = "invoices",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_invoice_number_org",
                        columnNames = {"invoice_number", "organization_id"}
                )
        }
)
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ======================
       Relationships
       ====================== */

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /* ======================
       Core Attributes
       ====================== */

    @NotNull
    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    private LocalDate issueDate;

    @NotNull
    @Column(nullable = false)
    private LocalDate dueDate;

    /* ======================
       Financials
       ====================== */

    @Column(precision = 15, scale = 2)
    private BigDecimal subtotal =  BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal taxInPercentage =  BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount =  BigDecimal.ZERO;

    /* ======================
       Composition
       ====================== */

    @OneToMany(
            mappedBy = "invoice",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<InvoiceItem> items = new ArrayList<>();

    /* ======================
       Audit
       ====================== */

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /* ======================
       JPA
       ====================== */

    protected Invoice() {
        // JPA only
    }

    /* ======================
       Factory
       ====================== */

    /**
     * Creates a new draft invoice. Issue date defaults to today.
     */
    public static Invoice create(Organization organization, String invoiceNumber, LocalDate dueDate, User createdBy) {
        return create(organization, invoiceNumber, LocalDate.now(), dueDate, createdBy, null);
    }

    /**
     * Creates a new draft invoice with optional customer.
     */
    public static Invoice create(Organization organization, String invoiceNumber, LocalDate issueDate,
                                LocalDate dueDate, User createdBy, Customer customer) {
        if (organization == null) {
            throw new IllegalArgumentException("Organization cannot be null");
        }
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            throw new IllegalArgumentException("Invoice number cannot be null or blank");
        }
        if (dueDate == null) {
            throw new IllegalArgumentException("Due date cannot be null");
        }
        if (createdBy == null) {
            throw new IllegalArgumentException("Created by user cannot be null");
        }
        if (issueDate != null && dueDate != null && dueDate.isBefore(issueDate)) {
            throw new IllegalArgumentException("Due date cannot be before issue date");
        }
        Invoice invoice = new Invoice();
        invoice.organization = organization;
        invoice.invoiceNumber = invoiceNumber.trim();
        invoice.status = InvoiceStatus.DRAFT;
        invoice.issueDate = issueDate != null ? issueDate : LocalDate.now();
        invoice.dueDate = dueDate;
        invoice.createdBy = createdBy;
        invoice.customer = customer;
        invoice.subtotal = BigDecimal.ZERO;
        invoice.taxInPercentage = BigDecimal.ZERO;
        invoice.totalAmount = BigDecimal.ZERO;
        invoice.items = new ArrayList<>();
        return invoice;
    }

    /* ======================
       Domain Behavior
       ====================== */

    /**
     * Adds an item to this invoice and recalculates totals.
     */
    public void addItem(String description, int quantity, BigDecimal unitPrice) {
        if (status != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Cannot add items to invoice with status " + status);
        }
        InvoiceItem item = InvoiceItem.create(this, description, quantity, unitPrice);
        items.add(item);
        recalculateTotals();
    }

    /**
     * Removes an item and recalculates totals.
     */
    public void removeItem(InvoiceItem item) {
        if (item == null) {
            return;
        }
        if (status != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Cannot remove items from invoice with status " + status);
        }
        if (items.remove(item)) {
            recalculateTotals();
        }
    }

    /**
     * Recalculates subtotal and total amount from items and tax percentage.
     */
    public void recalculateTotals() {
        this.subtotal = items.stream()
                .map(InvoiceItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = subtotal.multiply(taxInPercentage).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        this.totalAmount = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Sets the tax percentage and recalculates total amount.
     */
    public void setTaxPercentage(BigDecimal taxInPercentage) {
        if (taxInPercentage == null) {
            throw new IllegalArgumentException("Tax percentage cannot be null");
        }
        if (taxInPercentage.compareTo(BigDecimal.ZERO) < 0 || taxInPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Tax percentage must be between 0 and 100");
        }
        this.taxInPercentage = taxInPercentage.setScale(2, RoundingMode.HALF_UP);
        recalculateTotals();
    }

    /**
     * Sends the invoice (DRAFT → UNPAID). Only draft invoices can be sent.
     */
    public void send() {
        if (status != InvoiceStatus.DRAFT) {
            throw new IllegalStateException("Only draft invoices can be sent. Current status: " + status);
        }
        if (items.isEmpty()) {
            throw new IllegalStateException("Cannot send invoice without items");
        }
        this.status = InvoiceStatus.UNPAID;
    }

    /**
     * Marks the invoice as paid. Valid for UNPAID, PARTIALLY_PAID and OVERDUE.
     */
    public void markAsPaid() {
        if (status != InvoiceStatus.UNPAID && status != InvoiceStatus.PARTIALLY_PAID && status != InvoiceStatus.OVERDUE) {
            throw new IllegalStateException("Cannot mark as paid. Current status: " + status);
        }
        this.status = InvoiceStatus.PAID;
    }

    /**
     * Marks the invoice as partially paid.
     */
    public void markAsPartiallyPaid() {
        if (status != InvoiceStatus.UNPAID) {
            throw new IllegalStateException("Only unpaid invoices can be marked partially paid. Current status: " + status);
        }
        this.status = InvoiceStatus.PARTIALLY_PAID;
    }

    /**
     * Marks the invoice as overdue. Used by scheduler when due date has passed.
     */
    public void markAsOverdue() {
        if (status != InvoiceStatus.UNPAID) {
            throw new IllegalStateException("Only unpaid invoices can be marked overdue. Current status: " + status);
        }
        this.status = InvoiceStatus.OVERDUE;
    }

    /* ======================
       Callbacks
       ====================== */

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /* ======================
       Getters
       ====================== */

    public Long getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public Customer getCustomer() {
        return customer;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public InvoiceStatus getStatus() {
        return status;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getTaxInPercentage() {
        return taxInPercentage;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<InvoiceItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /* ======================
       Setters
       ====================== */

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public void setStatus(InvoiceStatus status) {
        this.status = status;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * @deprecated Use {@link #setTaxPercentage(BigDecimal)} which recalculates totals.
     */
    @Deprecated
    public void setTaxInPercentage(BigDecimal taxInPercentage) {
        setTaxPercentage(taxInPercentage);
    }

    /**
     * For JPA hydration only. Totals are derived via {@link #recalculateTotals()}.
     */
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal != null ? subtotal.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    /**
     * For JPA hydration only. Totals are derived via {@link #recalculateTotals()}.
     */
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount != null ? totalAmount.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    /**
     * For JPA / infrastructure only. Use {@link #addItem(String, int, BigDecimal)} and
     * {@link #removeItem(InvoiceItem)} for domain-driven changes.
     */
    public void setItems(List<InvoiceItem> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }
}
