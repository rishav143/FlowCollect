package com.flowcollect.api.v1.paymentlink;

import com.flowcollect.application.paymentlink.PaymentLinkService;
import com.flowcollect.domain.invoice.paymentlink.PaymentGateway;
import com.flowcollect.domain.invoice.paymentlink.PaymentLink;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Public endpoints for payment link redirection and gateway webhooks.
 *
 * All paths in this controller are excluded from JWT authentication
 * (configured in JwtFilter.SKIP_PREFIXES).
 *
 * Security model:
 *   - /pay/{token}             — token is an unguessable UUID; no further auth needed.
 *   - /api/v1/webhooks/stripe  — verified by Stripe-Signature header (HMAC-SHA256).
 *   - /api/v1/webhooks/razorpay — verified by X-Razorpay-Signature header (HMAC-SHA256).
 */
@RestController
public class PaymentLinkController {

    private final PaymentLinkService paymentLinkService;

    public PaymentLinkController(PaymentLinkService paymentLinkService) {
        this.paymentLinkService = paymentLinkService;
    }

    /**
     * Customer-facing redirect endpoint.
     * Looks up the PaymentLink by token and performs a 302 redirect to the gateway page.
     * Returns 410 Gone if the link has already been paid or expired.
     */
    @GetMapping("/pay/{token}")
    public void redirectToGateway(
            @PathVariable String token,
            HttpServletResponse response
    ) throws IOException {
        PaymentLink link = paymentLinkService.getByToken(token);
        if (!link.isActive()) {
            response.sendError(HttpServletResponse.SC_GONE,
                    "This payment link has expired or has already been fully paid.");
            return;
        }
        response.sendRedirect(link.getGatewayPaymentUrl());
    }

    /**
     * Stripe webhook receiver.
     * Stripe delivers signed `checkout.session.completed` events here.
     * Must return 200 quickly; heavy work happens inside a transaction.
     */
    @PostMapping("/api/v1/webhooks/stripe")
    public ResponseEntity<Void> stripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String stripeSignature
    ) {
        paymentLinkService.handleWebhook(PaymentGateway.STRIPE, payload, stripeSignature);
        return ResponseEntity.ok().build();
    }

    /**
     * Razorpay webhook receiver.
     * Razorpay delivers signed `payment_link.paid` events here.
     */
    @PostMapping("/api/v1/webhooks/razorpay")
    public ResponseEntity<Void> razorpayWebhook(
            @RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String razorpaySignature
    ) {
        paymentLinkService.handleWebhook(PaymentGateway.RAZORPAY, payload, razorpaySignature);
        return ResponseEntity.ok().build();
    }
}
