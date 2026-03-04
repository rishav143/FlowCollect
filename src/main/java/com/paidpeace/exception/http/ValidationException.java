package com.paidpeace.exception.http;

import org.springframework.http.HttpStatus;

import com.paidpeace.exception.base.BusinessException;
import com.paidpeace.exception.code.ErrorCode;

/**
 * Valid request format but violates business validation rules.
 */
public class ValidationException extends BusinessException {

    public ValidationException(ErrorCode code, String message) {
        super(code, message, HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
