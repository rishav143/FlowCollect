package com.flowcollect.domain.confirmation;

/**
 * Lifecycle status of a customer-submitted {@link PaymentConfirmation}.
 *
 * <ul>
 *   <li>{@link #PENDING_APPROVAL} — customer has submitted a payment claim; awaiting
 *       business review.</li>
 *   <li>{@link #APPROVED} — business confirmed the payment; the claimed amount has been
 *       recorded as a payment on the invoice.</li>
 *   <li>{@link #REMAINING_REQUESTED} — business approved a partial payment and sent an
 *       installment request to the customer for the outstanding balance.</li>
 *   <li>{@link #REJECTED} — business rejected the claim. The confirmation link remains
 *       OPEN so the customer may resubmit.</li>
 * </ul>
 */
public enum PaymentConfirmationStatus {
    PENDING_APPROVAL,
    APPROVED,
    REMAINING_REQUESTED,
    REJECTED
}
