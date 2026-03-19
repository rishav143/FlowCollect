package com.flowcollect.domain.organization;

/**
 * Determines how an organization collects payment from its customers on issued invoices.
 *
 * <ul>
 *   <li>{@link #PAYMENT_LINK} — customers pay directly via a gateway-hosted payment page.
 *       The {@code {{paymentLink}}} template placeholder is resolved to the gateway URL.</li>
 *   <li>{@link #CONFIRMATION_FLOW} — no gateway required. Customers self-report their payment
 *       via a lightweight confirmation page, and the business user approves or rejects the claim.
 *       The {@code {{confirmationLink}}} template placeholder is resolved to the confirmation URL.</li>
 * </ul>
 */
public enum PaymentCollectionMode {
    PAYMENT_LINK,
    CONFIRMATION_FLOW
}