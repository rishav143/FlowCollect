package com.flowcollect.api.v1.organization.dto;

import com.flowcollect.domain.invoice.paymentlink.PaymentGateway;
import com.flowcollect.domain.organization.gateway.GatewayConnectionStatus;
import com.flowcollect.domain.organization.gateway.OrganizationGatewayConfig;

import java.time.Instant;
import java.util.UUID;

/**
 * Safe read-only view of an OrganizationGatewayConfig.
 * Never exposes raw credentials — only connection metadata.
 */
public class GatewayConnectionResponse {

    private UUID id;
    private PaymentGateway gateway;
    private GatewayConnectionStatus status;

    /**
     * For Stripe: last 4 chars of the connected account ID (acct_...XXXX).
     * For Razorpay: masked key ID showing only the first 8 chars.
     * null if not connected.
     */
    private String accountHint;

    private Instant connectedAt;
    private Instant disconnectedAt;

    public static GatewayConnectionResponse from(OrganizationGatewayConfig config) {
        GatewayConnectionResponse r = new GatewayConnectionResponse();
        r.id = config.getId();
        r.gateway = config.getGateway();
        r.status = config.getStatus();
        r.connectedAt = config.getConnectedAt();
        r.disconnectedAt = config.getDisconnectedAt();

        if (config.getGateway() == PaymentGateway.STRIPE && config.getStripeAccountId() != null) {
            String acct = config.getStripeAccountId();
            r.accountHint = "acct_****" + acct.substring(Math.max(0, acct.length() - 4));
        }
        // For Razorpay: key ID is encrypted — we can't show it without decrypting,
        // but we know the user entered it, so show a generic masked hint
        if (config.getGateway() == PaymentGateway.RAZORPAY && config.getEncryptedKeyId() != null) {
            r.accountHint = "rzp_****";
        }

        return r;
    }

    public UUID getId() { return id; }
    public PaymentGateway getGateway() { return gateway; }
    public GatewayConnectionStatus getStatus() { return status; }
    public String getAccountHint() { return accountHint; }
    public Instant getConnectedAt() { return connectedAt; }
    public Instant getDisconnectedAt() { return disconnectedAt; }
}
