package com.flowcollect.exception.http;

import org.springframework.http.HttpStatus;

import com.flowcollect.exception.base.BusinessException;
import com.flowcollect.exception.code.ErrorCode;

/**
 * Rate limit exceeded.
 */
public class TooManyRequestsException extends BusinessException {

    public TooManyRequestsException(String message) {
        super(ErrorCode.TOO_MANY_REQUESTS, message, HttpStatus.TOO_MANY_REQUESTS);
    }
}
