package com.flowcollect.domain.organization.gateway;

import com.flowcollect.domain.invoice.paymentlink.PaymentGateway;
import com.flowcollect.domain.organization.Organization;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Stores a single organization's connection to one payment gateway.
 *
 * One row per (organization, gateway) pair — enforced by the unique constraint.
 *
 * Stripe Connect:
 *   stripeAccountId holds the connected account ID (acct_xxxx).
 *   Payments are created on behalf of this account; money lands in the
 *   organization's Stripe-linked bank account, not FlowCollect's.
 *
 * Razorpay (manual key entry):
 *   encryptedKeyId / encryptedKeySecret hold AES-256-GCM encrypted copies
 *   of the organization's Razorpay Key ID and Key Secret.
 *   webhookSecret is the per-org Razorpay webhook signing secret.
 *
 * Security:
 *   Credentials are encrypted at rest using a platform-level AES key
 *   (see GatewayCredentialEncryption). Never expose raw secrets in API responses.
 */
@Entity
@Table(
        name = "organization_gateway_configs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_org_gateway",
                        columnNames = {"organization_id", "gateway"}
                )
        }
)
public class OrganizationGatewayConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Relationships

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    // Core Attributes

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentGateway gateway;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GatewayConnectionStatus status;

    // Stripe Connect fields

    /** Stripe connected account ID (acct_xxxx). Null for Razorpay. */
    @Column(name = "stripe_account_id", length = 100)
    private String stripeAccountId;

    // Razorpay fields (AES-encrypted values)

    /** Encrypted Razorpay Key ID (rzp_live_... or rzp_test_...). Null for Stripe. */
    @Column(name = "encrypted_key_id", length = 500)
    private String encryptedKeyId;

    /** Encrypted Razorpay Key Secret. Null for Stripe. */
    @Column(name = "encrypted_key_secret", length = 500)
    private String encryptedKeySecret;

    /**
     * Per-organization webhook secret configured in the gateway dashboard.
     * Used to verify that incoming webhook calls are genuine.
     * For Razorpay: encrypted.
     * For Stripe: not needed here — Stripe Connect uses the platform webhook endpoint.
     */
    @Column(name = "webhook_secret", length = 500)
    private String webhookSecret;

    // Audit

    @Column(name = "connected_at")
    private Instant connectedAt;

    @Column(name = "disconnected_at")
    private Instant disconnectedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // JPA

    public OrganizationGatewayConfig() {}

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

    // Domain Behaviour

    public void connect() {
        this.status = GatewayConnectionStatus.CONNECTED;
        this.connectedAt = Instant.now();
        this.disconnectedAt = null;
    }

    public void disconnect() {
        this.status = GatewayConnectionStatus.DISCONNECTED;
        this.disconnectedAt = Instant.now();
        // Clear stored credentials on disconnect
        this.stripeAccountId = null;
        this.encryptedKeyId = null;
        this.encryptedKeySecret = null;
        this.webhookSecret = null;
    }

    public boolean isConnected() {
        return this.status == GatewayConnectionStatus.CONNECTED;
    }

    // Getters & Setters

    public UUID getId() { return id; }

    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization organization) { this.organization = organization; }

    public PaymentGateway getGateway() { return gateway; }
    public void setGateway(PaymentGateway gateway) { this.gateway = gateway; }

    public GatewayConnectionStatus getStatus() { return status; }

    public String getStripeAccountId() { return stripeAccountId; }
    public void setStripeAccountId(String stripeAccountId) { this.stripeAccountId = stripeAccountId; }

    public String getEncryptedKeyId() { return encryptedKeyId; }
    public void setEncryptedKeyId(String encryptedKeyId) { this.encryptedKeyId = encryptedKeyId; }

    public String getEncryptedKeySecret() { return encryptedKeySecret; }
    public void setEncryptedKeySecret(String encryptedKeySecret) { this.encryptedKeySecret = encryptedKeySecret; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public Instant getConnectedAt() { return connectedAt; }
    public Instant getDisconnectedAt() { return disconnectedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
