package com.flowcollect.domain.invoice;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.user.User;

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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relationships

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

    // Core Attributes
    @NotNull
    @Column(name = "invoice_number", nullable = false)
    private String invoiceNumber;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TimeStatus timeStatus = TimeStatus.NOT_DUE;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LifeCycleStatus lifeCycleStatus =  LifeCycleStatus.DRAFT;

    private LocalDate issueDate;

    private LocalDate dueDate;

    // Financials

    @Column(precision = 15, scale = 2)
    private BigDecimal subtotal =  BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    private BigDecimal taxInPercentage =  BigDecimal.ZERO;

    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount =  BigDecimal.ZERO;

    @Column(name = "total_paid", precision = 15, scale = 2)
    private BigDecimal totalPaid = BigDecimal.ZERO;

    // Composition

    @OneToMany(
            mappedBy = "invoice",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<InvoiceItem> items = new ArrayList<>();

    // Archive

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
    private boolean archived = false;

    // Audit

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    // JPA

    protected Invoice() {
        // JPA only
    }

    // Factory

    // Creates a new draft invoice with optional customer.
    public static Invoice create(Organization organization, String invoiceNumber) {
        if (organization == null) {
            throw new IllegalArgumentException("Organization cannot be null");
        }
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            throw new IllegalArgumentException("Invoice number cannot be null or blank");
        }
        Invoice invoice = new Invoice();
        invoice.organization = organization;
        invoice.invoiceNumber = invoiceNumber.trim();
        invoice.items = new ArrayList<>();
        return invoice;
    }

    // Callbacks

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters

    public UUID getId() {
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

    public TimeStatus getTimeStatus() {
        return timeStatus;
    }

    public LifeCycleStatus getLifeCycleStatus() {
        return lifeCycleStatus;
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

    public BigDecimal getTotalPaid() {
        return totalPaid != null ? totalPaid : BigDecimal.ZERO;
    }

    public List<InvoiceItem> getItems() {
        return items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public boolean isDraft() {
        return lifeCycleStatus == LifeCycleStatus.DRAFT;
    }

    public boolean isIssued() {
        return lifeCycleStatus == LifeCycleStatus.ISSUED;
    }

    public boolean isPartiallyPaid() {
        return lifeCycleStatus == LifeCycleStatus.PARTIALLY_PAID;
    }

    public boolean isPaid() {
        return lifeCycleStatus == LifeCycleStatus.PAID;
    }

    public boolean isCancelled() {
        return lifeCycleStatus == LifeCycleStatus.CANCELLED;
    }

    public boolean isOverdue() {
        return timeStatus == TimeStatus.OVERDUE;
    }

    public boolean isArchived() {
        return archived;
    }

    // Setters

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

    public void setIssueDate(LocalDate issueDate) {
        if (issueDate == null) {
            throw new IllegalArgumentException("Issue date cannot be null");
        }
        if (lifeCycleStatus != LifeCycleStatus.DRAFT) {
            throw new IllegalStateException("Only draft invoices can be issued. Current status: " + lifeCycleStatus);
        }
        if(totalAmount.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Total amount cannot be zero or negative");
        }
        this.issueDate = issueDate;
        this.lifeCycleStatus = LifeCycleStatus.ISSUED;
    }

    public void setIssuedNow(ZoneId timezone) {
        if (lifeCycleStatus != LifeCycleStatus.DRAFT) {
            throw new IllegalStateException("Only draft invoices can be issued. Current status: " + lifeCycleStatus);
        }
        if(totalAmount.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException("Total amount cannot be zero or negative");
        }
        this.issueDate = LocalDate.now(timezone);
        this.lifeCycleStatus = LifeCycleStatus.ISSUED;
    }

    public void markAsCancelled() {
        this.lifeCycleStatus = LifeCycleStatus.CANCELLED;
    }

    public void archive() {
        if (lifeCycleStatus != LifeCycleStatus.PAID && lifeCycleStatus != LifeCycleStatus.CANCELLED) {
            throw new IllegalStateException("Only PAID or CANCELLED invoices can be archived. Current status: " + lifeCycleStatus);
        }
        this.archived = true;
    }

    public void unarchive() {
        this.archived = false;
    }

    public void updateLifeCycleStatus(BigDecimal totalPaid) {
        if (lifeCycleStatus == LifeCycleStatus.DRAFT || lifeCycleStatus == LifeCycleStatus.CANCELLED) {
            return;
        }

        this.totalPaid = totalPaid.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        if (totalPaid.compareTo(BigDecimal.ZERO) <= 0) {
            this.lifeCycleStatus = LifeCycleStatus.ISSUED;
        } else if (totalPaid.compareTo(this.totalAmount) >= 0) {
            this.lifeCycleStatus = LifeCycleStatus.PAID;
            this.timeStatus = TimeStatus.NOT_DUE; // Paid invoices are never overdue
        } else {
            this.lifeCycleStatus = LifeCycleStatus.PARTIALLY_PAID;
        }
    }

    public BigDecimal getRemainingAmount() {
        BigDecimal paid = this.totalPaid != null ? this.totalPaid : BigDecimal.ZERO;
        return this.totalAmount.subtract(paid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public void setDueDate(LocalDate dueDate) {
        if (dueDate == null) {
            throw new IllegalArgumentException("Due date cannot be null");
        }
        this.dueDate = dueDate;
    }

    public void refreshTimeStatus(LocalDate referenceDate) {
        if (referenceDate == null) {
            throw new IllegalArgumentException("Reference date cannot be null");
        }
        if (this.dueDate == null) {
            this.timeStatus = TimeStatus.NOT_DUE;
            return;
        }
        if (this.dueDate.isEqual(referenceDate)) {
            this.timeStatus = TimeStatus.DUE_TODAY;
        } else if (this.dueDate.isBefore(referenceDate)) {
            this.timeStatus = TimeStatus.OVERDUE;
        } else {
            this.timeStatus = TimeStatus.NOT_DUE;
        }
    }

    // Domain Behavior
    // Adds an item to this invoice and recalculates totals.
    public void addItem(String description, int quantity, BigDecimal unitPrice) {
        if (lifeCycleStatus != LifeCycleStatus.DRAFT) {
            throw new IllegalStateException("Cannot add items to invoice with status " + lifeCycleStatus);
        }
        InvoiceItem item = InvoiceItem.create(this, description, quantity, unitPrice);
        items.add(item);
        this.subtotal = this.subtotal
                .add(item.getAmount())
                .setScale(2, RoundingMode.HALF_UP);
        calculateTotal();
    }

    // Removes an item and recalculates totals.
    public void removeItem(InvoiceItem item) {
        if (item == null) {
            return;
        }
        if (lifeCycleStatus != LifeCycleStatus.DRAFT) {
            throw new IllegalStateException("Cannot remove items from invoice with status " + lifeCycleStatus);
        }
        if (items.remove(item)) {
            this.subtotal = this.subtotal
                    .subtract(item.getAmount())
                    .max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            calculateTotal();
        }
    }

    // Sets the tax percentage and recalculates total amount.
    public void setTaxPercentage(BigDecimal taxInPercentage) {
        if (taxInPercentage == null) {
            throw new IllegalArgumentException("Tax percentage cannot be null");
        }
        if (taxInPercentage.compareTo(BigDecimal.ZERO) < 0 || taxInPercentage.compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Tax percentage must be between 0 and 100");
        }
        this.taxInPercentage = taxInPercentage.setScale(2, RoundingMode.HALF_UP);
        calculateTotal();
    }

    // Calculates the total amount of the invoice.
    public void calculateTotal() {
        BigDecimal taxAmount = this.subtotal.multiply(this.taxInPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        this.totalAmount = this.subtotal
                .add(taxAmount)
                .max(BigDecimal.ZERO)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
