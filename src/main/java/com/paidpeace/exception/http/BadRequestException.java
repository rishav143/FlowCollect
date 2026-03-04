package com.paidpeace.exception.http;

import org.springframework.http.HttpStatus;

import com.paidpeace.exception.base.BusinessException;
import com.paidpeace.exception.code.ErrorCode;

/**
 * Invalid input provided by client.
 * Example: invalid currency, invalid field, invalid state
 */
public class BadRequestException extends BusinessException {

    public BadRequestException(ErrorCode code, String message) {
        super(code, message, HttpStatus.BAD_REQUEST);
    }
}
