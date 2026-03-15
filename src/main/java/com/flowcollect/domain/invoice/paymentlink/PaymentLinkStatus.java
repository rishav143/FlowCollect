package com.flowcollect.domain.invoice.paymentlink;

/**
 * Lifecycle status of a payment link.
 *
 * ACTIVE         - link is valid and ready to accept payment.
 * PARTIALLY_PAID - customer paid less than the full amount due.
 * PAID           - full amount was collected via this link.
 * EXPIRED        - link reached its expiry date without full payment.
 * CANCELLED      - link was manually cancelled.
 */
public enum PaymentLinkStatus {
    ACTIVE,
    PARTIALLY_PAID,
    PAID,
    EXPIRED,
    CANCELLED
}
