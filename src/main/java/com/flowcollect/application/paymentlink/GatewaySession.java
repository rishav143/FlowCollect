package com.flowcollect.application.paymentlink;

/**
 * Value object returned by a PaymentGatewayProvider after successfully
 * creating a remote payment session or link.
 *
 * @param sessionId  Gateway-specific identifier used to match incoming webhooks
 *                   (Stripe Checkout Session ID, Razorpay Payment Link ID).
 * @param paymentUrl Direct URL on the gateway side to redirect the customer to.
 */
public record GatewaySession(String sessionId, String paymentUrl) {}
