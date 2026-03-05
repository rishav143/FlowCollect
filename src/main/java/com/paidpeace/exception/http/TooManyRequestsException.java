package com.paidpeace.exception.http;

import org.springframework.http.HttpStatus;

import com.paidpeace.exception.base.BusinessException;
import com.paidpeace.exception.code.ErrorCode;

/**
 * Rate limit exceeded.
 */
public class TooManyRequestsException extends BusinessException {

    public TooManyRequestsException(String message) {
        super(ErrorCode.TOO_MANY_REQUESTS, message, HttpStatus.TOO_MANY_REQUESTS);
    }
}
