package com.cashclarity.exception.base;

import org.springframework.http.HttpStatus;

import com.cashclarity.api.error.ErrorCode;

/**
 * Root class for all business/application exceptions.
 * Stores stable error code + HTTP status.
 */
public abstract class AppException extends RuntimeException {

    private final ErrorCode code;
    private final HttpStatus status;

    protected AppException(ErrorCode code, String message, HttpStatus status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    /** Machine readable error code (for frontend / logs) */
    public ErrorCode getCode() {
        return code;
    }

    /** HTTP status mapped automatically by GlobalExceptionHandler */
    public HttpStatus getStatus() {
        return status;
    }
}