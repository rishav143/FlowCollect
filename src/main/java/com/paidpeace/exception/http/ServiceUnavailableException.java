package com.paidpeace.exception.http;

import org.springframework.http.HttpStatus;

import com.paidpeace.exception.base.BusinessException;
import com.paidpeace.exception.code.ErrorCode;

/**
 * Temporary failure, retry later.
 */
public class ServiceUnavailableException extends BusinessException {

    public ServiceUnavailableException(ErrorCode code, String message) {
        super(code, message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
