package com.flowcollect.application.paymentlink;

import java.math.BigDecimal;

/**
 * Normalised representation of a payment event received from a gateway webhook.
 * Each PaymentGatewayProvider translates its raw payload into this contract.
 *
 * @param sessionId            Gateway session/link ID that maps back to our PaymentLink record.
 * @param amountPaid           Amount paid in the invoice currency (already decimal, e.g. 50.00).
 * @param currency             ISO 4217 currency code reported by the gateway.
 * @param successful           Whether the payment was successful.
 * @param transactionReference Gateway-side payment/transaction ID used for idempotency.
 */
public record WebhookEvent(
        String sessionId,
        BigDecimal amountPaid,
        String currency,
        boolean successful,
        String transactionReference
) {}
