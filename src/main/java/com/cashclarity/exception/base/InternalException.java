package com.cashclarity.exception.base;

import org.springframework.http.HttpStatus;

import com.cashclarity.api.error.ErrorCode;

/**
 * Unexpected system condition that we explicitly detect.
 */
public abstract class InternalException extends AppException {

    protected InternalException(ErrorCode code, String message) {
        super(code, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
