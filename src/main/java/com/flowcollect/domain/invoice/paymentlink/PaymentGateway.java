package com.flowcollect.domain.invoice.paymentlink;

/**
 * Supported payment gateways for generating payment links.
 * STRIPE  - card / wallet payments, all currencies.
 * RAZORPAY - UPI, card, netbanking; primarily for INR.
 */
public enum PaymentGateway {
    STRIPE,
    RAZORPAY
}
