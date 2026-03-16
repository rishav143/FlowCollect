package com.flowcollect.exception.http;

import org.springframework.http.HttpStatus;

import com.flowcollect.exception.base.BusinessException;
import com.flowcollect.exception.code.ErrorCode;

/**
 * Temporary failure, retry later.
 */
public class ServiceUnavailableException extends BusinessException {

    public ServiceUnavailableException(String message) {
        super(ErrorCode.SERVICE_UNAVAILABLE, message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
