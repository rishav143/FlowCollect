package com.cashclarity.exception;

/**
 * Base exception for all application-level errors.
 * Custom exceptions in the service layer extend this so the controller
 * can handle them uniformly via {@link GlobalExceptionHandler}.
 */
public class CashClarityException extends RuntimeException {

    public CashClarityException(String message) {
        super(message);
    }

    public CashClarityException(String message, Throwable cause) {
        super(message, cause);
    }
}
