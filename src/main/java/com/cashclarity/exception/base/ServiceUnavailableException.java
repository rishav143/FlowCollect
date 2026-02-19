package com.cashclarity.exception.base;

import org.springframework.http.HttpStatus;

import com.cashclarity.api.error.ErrorCode;

/**
 * Temporary failure, retry later.
 */
public abstract class ServiceUnavailableException extends AppException {

    protected ServiceUnavailableException(ErrorCode code, String message) {
        super(code, message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
