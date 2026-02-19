package com.cashclarity.exception.base;

import org.springframework.http.HttpStatus;

import com.cashclarity.api.error.ErrorCode;

/**
 * Rate limit exceeded.
 */
public abstract class TooManyRequestsException extends AppException {

    protected TooManyRequestsException(ErrorCode code, String message) {
        super(code, message, HttpStatus.TOO_MANY_REQUESTS);
    }
}
