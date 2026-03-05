package com.paidpeace.exception.http;

import org.springframework.http.HttpStatus;

import com.paidpeace.exception.base.BusinessException;
import com.paidpeace.exception.code.ErrorCode;

/**
 * Valid request format but violates business validation rules.
 */
public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super(ErrorCode.UNPROCESSABLE_CONTENT, message, HttpStatus.UNPROCESSABLE_CONTENT);
    }
}
