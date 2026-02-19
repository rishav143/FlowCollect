package com.cashclarity.exception.base;

import org.springframework.http.HttpStatus;

import com.cashclarity.api.error.ErrorCode;

/**
 * Authenticated but not allowed to perform action.
 */
public abstract class ForbiddenException extends AppException {

    protected ForbiddenException(ErrorCode code, String message) {
        super(code, message, HttpStatus.FORBIDDEN);
    }
}
