package com.flowcollect.application.paymentlink;

import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.paymentlink.PaymentGateway;
import com.flowcollect.domain.organization.gateway.OrganizationGatewayConfig;
import com.flowcollect.exception.http.InternalException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.config.StripeProperties;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Stripe implementation of PaymentGatewayProvider using Stripe Connect.
 *
 * Each organization connects their own Stripe account. Sessions are created
 * on behalf of the organization's connected account, so:
 *   - Payments land directly in the organization's Stripe-linked bank account.
 *   - FlowCollect never holds or processes the funds.
 *
 * Platform webhook: Stripe sends a single webhook to FlowCollect's endpoint.
 * We identify the owning org via the connected account ID in the event.
 *
 * Only active when stripe.enabled=true.
 */
@Component
@ConditionalOnProperty(name = "stripe.enabled", havingValue = "true")
public class StripePaymentGatewayProvider implements PaymentGatewayProvider {

    private final StripeProperties stripeProperties;

    public StripePaymentGatewayProvider(StripeProperties stripeProperties) {
        this.stripeProperties = stripeProperties;
    }

    @Override
    public PaymentGateway gateway() {
        return PaymentGateway.STRIPE;
    }

    /**
     * Creates a Stripe Checkout Session on behalf of the org's connected account.
     * The session URL is the hosted Stripe checkout page where the customer pays.
     * Money goes directly to the connected account (org's bank).
     */
    @Override
    public GatewaySession createSession(
            Invoice invoice,
            String publicPaymentUrl,
            OrganizationGatewayConfig config
    ) {
        if (config.getStripeAccountId() == null) {
            throw new ValidationException(
                    "Organization has not connected a Stripe account. " +
                    "Complete Stripe Connect setup in Settings → Payment Gateways.");
        }

        Stripe.apiKey = stripeProperties.getApiKey();

        // Route this session through the org's connected account
        RequestOptions requestOptions = RequestOptions.builder()
                .setStripeAccount(config.getStripeAccountId())
                .build();

        long amountInSmallestUnit = invoice.getTotalAmount()
                .multiply(BigDecimal.valueOf(100))
                .longValue();

        String currencyCode = invoice.getOrganization().getCurrency().getCurrencyCode().toLowerCase();

        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(publicPaymentUrl + "/success")
                    .setCancelUrl(publicPaymentUrl + "/cancel")
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(currencyCode)
                                                    .setUnitAmount(amountInSmallestUnit)
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Invoice " + invoice.getInvoiceNumber())
                                                                    .setDescription("Payment to " + invoice.getOrganization().getName())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .build();

            // Pass requestOptions so this session is charged to the connected account
            Session session = Session.create(params, requestOptions);
            return new GatewaySession(session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new InternalException("Stripe session creation failed: " + e.getMessage());
        }
    }

    /**
     * Verifies and parses a Stripe Connect webhook.
     *
     * Stripe sends Connect webhooks with the platform's webhook secret
     * (not per-org secrets). The connected account ID in the event lets us
     * identify which organization this payment belongs to.
     */
    @Override
    public WebhookEvent parseWebhook(
            String payload,
            String signatureHeader,
            OrganizationGatewayConfig config
    ) {
        // Stripe Connect uses the platform-level webhook secret for all connected account events
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, stripeProperties.getWebhookSecret());
        } catch (SignatureVerificationException e) {
            throw new ValidationException("Invalid Stripe webhook signature");
        }

        if (!"checkout.session.completed".equals(event.getType())) {
            return null;
        }

        StripeObject stripeObject = event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new InternalException("Failed to deserialise Stripe webhook payload"));

        Session session = (Session) stripeObject;

        BigDecimal amountPaid = BigDecimal.valueOf(session.getAmountTotal())
                .divide(BigDecimal.valueOf(100));

        return new WebhookEvent(
                session.getId(),
                amountPaid,
                session.getCurrency().toUpperCase(),
                true,
                session.getPaymentIntent()
        );
    }
}
