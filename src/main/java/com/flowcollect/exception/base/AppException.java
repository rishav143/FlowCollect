package com.flowcollect.exception.base;

import org.springframework.http.HttpStatus;

import com.flowcollect.exception.code.ErrorCode;

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

    public AppException(String message, HttpStatus status) {
        super(message);
        this.code = null;
        this.status = status;
    }

    public ErrorCode getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}