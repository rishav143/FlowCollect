package com.flowcollect.application.paymentlink;

import com.flowcollect.application.organization.GatewayCredentialEncryption;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.paymentlink.PaymentGateway;
import com.flowcollect.domain.organization.gateway.OrganizationGatewayConfig;
import com.flowcollect.exception.http.InternalException;
import com.flowcollect.exception.http.ValidationException;

import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;

import org.json.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Razorpay implementation of PaymentGatewayProvider.
 *
 * Each organization stores their own Razorpay Key ID and Secret (encrypted at rest).
 * Payment links are created using the org's own credentials so payments land
 * directly in their Razorpay-linked bank account.
 *
 * Supports UPI, cards, netbanking — primarily for INR but Razorpay also
 * supports other currencies via international payments.
 *
 * Only active when razorpay.enabled=true.
 */
@Component
@ConditionalOnProperty(name = "razorpay.enabled", havingValue = "true")
public class RazorpayPaymentGatewayProvider implements PaymentGatewayProvider {

    private final GatewayCredentialEncryption encryption;

    public RazorpayPaymentGatewayProvider(GatewayCredentialEncryption encryption) {
        this.encryption = encryption;
    }

    @Override
    public PaymentGateway gateway() {
        return PaymentGateway.RAZORPAY;
    }

    /**
     * Creates a Razorpay Payment Link using the organization's own API credentials.
     * The payment is collected directly into the organization's Razorpay account
     * and then settled to their linked bank account.
     */
    @Override
    public GatewaySession createSession(
            Invoice invoice,
            String publicPaymentUrl,
            OrganizationGatewayConfig config
    ) {
        if (config.getEncryptedKeyId() == null || config.getEncryptedKeySecret() == null) {
            throw new ValidationException(
                    "Organization has not connected a Razorpay account. " +
                    "Go to Settings → Payment Gateways to add your Razorpay API keys.");
        }

        // Decrypt the stored credentials for this org
        String keyId = encryption.decrypt(config.getEncryptedKeyId());
        String keySecret = encryption.decrypt(config.getEncryptedKeySecret());

        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            long amountInSmallestUnit = invoice.getTotalAmount()
                    .multiply(BigDecimal.valueOf(100))
                    .longValue();

            JSONObject params = new JSONObject();
            params.put("amount", amountInSmallestUnit);
            params.put("currency", invoice.getOrganization().getCurrency().getCurrencyCode());
            params.put("accept_partial", false);
            params.put("description", "Invoice " + invoice.getInvoiceNumber()
                    + " — " + invoice.getOrganization().getName());

            // Disable Razorpay's built-in notifications — FlowCollect handles delivery
            JSONObject notify = new JSONObject();
            notify.put("sms", false);
            notify.put("email", false);
            params.put("notify", notify);

            params.put("callback_url", publicPaymentUrl + "/success");
            params.put("callback_method", "get");

            com.razorpay.PaymentLink paymentLink = client.paymentLink.create(params);
            return new GatewaySession(paymentLink.get("id"), paymentLink.get("short_url"));
        } catch (RazorpayException e) {
            throw new InternalException("Razorpay payment link creation failed: " + e.getMessage());
        }
    }

    /**
     * Verifies and parses a Razorpay webhook using the organization's
     * per-org webhook secret (stored encrypted in OrganizationGatewayConfig).
     */
    @Override
    public WebhookEvent parseWebhook(
            String payload,
            String signatureHeader,
            OrganizationGatewayConfig config
    ) {
        // Decrypt the org's webhook secret for verification
        String webhookSecret = encryption.decrypt(config.getWebhookSecret());

        try {
            boolean valid = Utils.verifyWebhookSignature(payload, signatureHeader, webhookSecret);
            if (!valid) {
                throw new ValidationException("Invalid Razorpay webhook signature");
            }
        } catch (RazorpayException e) {
            throw new ValidationException("Razorpay webhook verification failed: " + e.getMessage());
        }

        JSONObject body = new JSONObject(payload);
        String eventType = body.getString("event");

        if (!"payment_link.paid".equals(eventType)) {
            return null;
        }

        JSONObject payloadNode = body.getJSONObject("payload");

        JSONObject linkEntity = payloadNode
                .getJSONObject("payment_link")
                .getJSONObject("entity");

        JSONObject paymentEntity = payloadNode
                .getJSONObject("payment")
                .getJSONObject("entity");

        String linkId = linkEntity.getString("id");
        long amountPaidSmallestUnit = linkEntity.getLong("amount_paid");
        String currency = linkEntity.getString("currency");
        String paymentId = paymentEntity.getString("id");

        BigDecimal amountPaid = BigDecimal.valueOf(amountPaidSmallestUnit)
                .divide(BigDecimal.valueOf(100));

        return new WebhookEvent(linkId, amountPaid, currency, true, paymentId);
    }
}
