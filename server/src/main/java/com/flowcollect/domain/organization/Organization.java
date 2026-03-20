package com.flowcollect.domain.organization;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.UUID;

@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    // Core Identity
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String name;

    // Contact Information
    @Email
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, unique = true)
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String address;

    // Localization & Finance
    @NotNull
    @Column(nullable = false, length = 50)
    private ZoneId timezone;

    @NotNull
    @Column(nullable = false, length = 3)
    private Currency currency;

    // Branding
    @Size(max = 255)
    private String logoUrl;

    // Verification
    @Column(nullable = false)
    private boolean phoneVerified = false;

    // Status
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    /**
     * How this organization collects payment from customers.
     * Defaults to {@link PaymentCollectionMode#PAYMENT_LINK} for backward compatibility
     * with organizations created before this field existed.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_collection_mode", nullable = false, length = 20)
    private PaymentCollectionMode paymentCollectionMode = PaymentCollectionMode.PAYMENT_LINK;

    // Audit
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column(updatable = false)
    private Long createdBy;

    private Long updatedBy;

    private Instant deletedAt;

    // Constructors
    protected Organization() {
        // Required by JPA
    }

    public Organization(String name, String email, ZoneId timezone, Currency currency) {
        this.name = name;
        this.email = email;
        this.timezone = timezone;
        this.currency = currency;
        this.status = OrganizationStatus.ACTIVE;
    }

    // Callbacks (audit only)
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

    public Long getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public ZoneId getTimezone() {
        return timezone;
    }

    public Currency getCurrency() {
        return currency;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public OrganizationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public Long getUpdatedBy() {
        return updatedBy;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public PaymentCollectionMode getPaymentCollectionMode() {
        return paymentCollectionMode;
    }

    public void setPaymentCollectionMode(PaymentCollectionMode paymentCollectionMode) {
        this.paymentCollectionMode = paymentCollectionMode;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isPhoneVerified() {
        return phoneVerified;
    }

    public void verifyPhone() {
        this.phoneVerified = true;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setTimezone(ZoneId timezone) {
        this.timezone = timezone;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public void setUpdatedBy(Long updatedBy) {
        this.updatedBy = updatedBy;
    }

    // Business Logic Methods
    public void activate() {
        this.status = OrganizationStatus.ACTIVE;
    }

    public void suspend() {
        this.status = OrganizationStatus.SUSPENDED;
    }

    public void archive() {
        this.status = OrganizationStatus.ARCHIVED;
        this.deletedAt = Instant.now();
    }

    public boolean isActive() {
        return this.status == OrganizationStatus.ACTIVE;
    }

    public void expired() {
        this.status = OrganizationStatus.EXPIRED;
    }

    public void trial() {
        this.status = OrganizationStatus.TRIAL;
    }
}