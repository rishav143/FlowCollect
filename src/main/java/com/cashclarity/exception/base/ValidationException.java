package com.cashclarity.exception.base;

import org.springframework.http.HttpStatus;

import com.cashclarity.api.error.ErrorCode;

/**
 * Valid request format but violates business validation rules.
 */
public abstract class ValidationException extends AppException {

    protected ValidationException(ErrorCode code, String message) {
        super(code, message, HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
