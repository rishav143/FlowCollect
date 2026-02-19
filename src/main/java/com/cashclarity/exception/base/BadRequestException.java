package com.cashclarity.exception.base;

import org.springframework.http.HttpStatus;

import com.cashclarity.api.error.ErrorCode;

/**
 * Invalid input provided by client.
 * Example: invalid currency, invalid field, invalid state
 */
public abstract class BadRequestException extends AppException {

    protected BadRequestException(ErrorCode code, String message) {
        super(code, message, HttpStatus.BAD_REQUEST);
    }
}
