package com.flowcollect.application.paymentlink;

import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.paymentlink.PaymentGateway;
import com.flowcollect.domain.organization.gateway.OrganizationGatewayConfig;

/**
 * Strategy interface for interacting with an external payment gateway.
 *
 * Each implementation handles one gateway (Stripe, Razorpay, …) and is
 * responsible for:
 *   1. Creating a remote payment session/link for a given invoice.
 *   2. Verifying and parsing raw webhook payloads into a normalised WebhookEvent.
 *
 * Implementations are Spring components collected at startup by PaymentLinkService.
 */
public interface PaymentGatewayProvider {

    /** Returns the gateway this provider handles. */
    PaymentGateway gateway();

    /**
     * Creates a payment session on the gateway for the given invoice.
     *
     * The session is created on behalf of the organization's own gateway account
     * (Stripe connected account / Razorpay API keys) so that money lands directly
     * in their bank — not in FlowCollect's account.
     *
     * @param invoice          The invoice to be paid.
     * @param publicPaymentUrl Our stable public URL for this link (used as success/cancel base URL).
     * @param config           The organization's connected gateway credentials.
     * @return A GatewaySession containing the remote session ID and the URL to redirect customers to.
     */
    GatewaySession createSession(Invoice invoice, String publicPaymentUrl, OrganizationGatewayConfig config);

    /**
     * Verifies and parses a raw webhook payload received from the gateway.
     *
     * @param payload         Raw request body as a string.
     * @param signatureHeader Gateway-specific signature header value.
     * @param config          The organization's gateway config (for per-org webhook secret lookup).
     * @return A WebhookEvent if the event represents a payment we should record,
     *         or {@code null} if the event type is unrecognised / not actionable.
     * @throws com.flowcollect.exception.http.ValidationException if signature verification fails.
     */
    WebhookEvent parseWebhook(String payload, String signatureHeader, OrganizationGatewayConfig config);
}
