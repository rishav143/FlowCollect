package com.cashclarity.exception;

/**
 * Thrown when the provided currency code is not valid (e.g. not a valid ISO 4217 code).
 */
public class InvalidCurrencyException extends CashClarityException {

    public InvalidCurrencyException(String currencyCode) {
        super("Invalid currency: '" + currencyCode + "'. Provide a valid ISO 4217 currency code (e.g. USD, EUR, INR).");
    }

    public InvalidCurrencyException(String currencyCode, Throwable cause) {
        super("Invalid currency: '" + currencyCode + "'. Provide a valid ISO 4217 currency code (e.g. USD, EUR, INR).", cause);
    }
}
