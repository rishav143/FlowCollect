package com.cashclarity.exception.base;

import org.springframework.http.HttpStatus;

import com.cashclarity.api.error.ErrorCode;

/**
 * Authentication required or token invalid.
 */
public abstract class UnauthorizedException extends AppException {

    protected UnauthorizedException(ErrorCode code, String message) {
        super(code, message, HttpStatus.UNAUTHORIZED);
    }
}
