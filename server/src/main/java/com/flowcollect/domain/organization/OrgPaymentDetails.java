package com.flowcollect.domain.organization;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

/**
 * Payment receiving details for an organisation.
 * Shown on the public confirmation page so customers know where to send money.
 *
 * One row per org (unique FK). All fields are nullable — fill only what applies.
 *
 * Coverage:
 *   accountNumber  → domestic (US, India, AU, etc.)
 *   iban           → Europe, UK, Middle East (SEPA + international)
 *   swiftCode      → cross-border wire (SWIFT / BIC)
 *   routingNumber  → US ACH
 *   ifscCode       → India NEFT / RTGS
 *   upiId          → India UPI
 *   paypalEmail    → PayPal (global)
 *   wiseEmail      → Wise (global, popular with freelancers)
 */
@Entity
@Table(name = "org_payment_details")
public class OrgPaymentDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "org_id", nullable = false, unique = true)
    private UUID orgId;

    // ── Bank / Wire Transfer ──────────────────────────────────────────────────

    @Size(max = 100)
    @Column(name = "bank_name")
    private String bankName;

    @Size(max = 100)
    @Column(name = "account_holder_name")
    private String accountHolderName;

    /** Domestic account number — US, India, Australia, etc. */
    @Size(max = 50)
    @Column(name = "account_number")
    private String accountNumber;

    /** IBAN — Europe, UK, Middle East */
    @Size(max = 34)
    @Column(name = "iban")
    private String iban;

    /** SWIFT / BIC — international wire transfers */
    @Size(max = 11)
    @Column(name = "swift_code")
    private String swiftCode;

    /** US ACH routing number */
    @Size(max = 20)
    @Column(name = "routing_number")
    private String routingNumber;

    /** India NEFT / RTGS */
    @Size(max = 20)
    @Column(name = "ifsc_code")
    private String ifscCode;

    // ── India UPI ─────────────────────────────────────────────────────────────

    @Size(max = 100)
    @Column(name = "upi_id")
    private String upiId;

    // ── Digital Wallets ───────────────────────────────────────────────────────

    @Size(max = 100)
    @Column(name = "paypal_email")
    private String paypalEmail;

    @Size(max = 100)
    @Column(name = "wise_email")
    private String wiseEmail;

    // ── Freeform note ─────────────────────────────────────────────────────────

    @Size(max = 300)
    @Column(name = "additional_note")
    private String additionalNote;

    // ── Audit ─────────────────────────────────────────────────────────────────

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected OrgPaymentDetails() {}

    public OrgPaymentDetails(UUID orgId) {
        this.orgId = orgId;
    }

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
    public UUID getId()                   { return id; }
    public UUID getOrgId()                { return orgId; }
    public String getBankName()           { return bankName; }
    public String getAccountHolderName()  { return accountHolderName; }
    public String getAccountNumber()      { return accountNumber; }
    public String getIban()               { return iban; }
    public String getSwiftCode()          { return swiftCode; }
    public String getRoutingNumber()      { return routingNumber; }
    public String getIfscCode()           { return ifscCode; }
    public String getUpiId()              { return upiId; }
    public String getPaypalEmail()        { return paypalEmail; }
    public String getWiseEmail()          { return wiseEmail; }
    public String getAdditionalNote()     { return additionalNote; }
    public Instant getCreatedAt()         { return createdAt; }
    public Instant getUpdatedAt()         { return updatedAt; }

    // Setters
    public void setBankName(String v)          { this.bankName = v; }
    public void setAccountHolderName(String v) { this.accountHolderName = v; }
    public void setAccountNumber(String v)     { this.accountNumber = v; }
    public void setIban(String v)              { this.iban = v; }
    public void setSwiftCode(String v)         { this.swiftCode = v; }
    public void setRoutingNumber(String v)     { this.routingNumber = v; }
    public void setIfscCode(String v)          { this.ifscCode = v; }
    public void setUpiId(String v)             { this.upiId = v; }
    public void setPaypalEmail(String v)       { this.paypalEmail = v; }
    public void setWiseEmail(String v)         { this.wiseEmail = v; }
    public void setAdditionalNote(String v)    { this.additionalNote = v; }

    /** True if at least one meaningful payment detail is populated. */
    public boolean hasAnyDetail() {
        return isNotBlank(bankName)      || isNotBlank(accountNumber)  ||
               isNotBlank(iban)          || isNotBlank(swiftCode)      ||
               isNotBlank(routingNumber) || isNotBlank(ifscCode)       ||
               isNotBlank(upiId)         || isNotBlank(paypalEmail)    ||
               isNotBlank(wiseEmail);
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }
}
