package com.cashclarity.exception.base;

import org.springframework.http.HttpStatus;

import com.cashclarity.api.error.ErrorCode;

/**
 * Resource does not exist.
 */
public abstract class NotFoundException extends AppException {

    protected NotFoundException(ErrorCode code, String message) {
        super(code, message, HttpStatus.NOT_FOUND);
    }
}
