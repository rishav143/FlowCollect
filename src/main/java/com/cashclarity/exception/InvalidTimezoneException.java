package com.cashclarity.exception;

/**
 * Thrown when the provided timezone identifier is not valid (e.g. not a valid ZoneId).
 */
public class InvalidTimezoneException extends CashClarityException {

    public InvalidTimezoneException(String timezone) {
        super("Invalid timezone: '" + timezone + "'. Provide a valid timezone ID (e.g. America/New_York, Europe/London).");
    }

    public InvalidTimezoneException(String timezone, Throwable cause) {
        super("Invalid timezone: '" + timezone + "'. Provide a valid timezone ID (e.g. America/New_York, Europe/London).", cause);
    }
}
