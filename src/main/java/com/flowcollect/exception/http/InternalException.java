package com.flowcollect.exception.http;

import org.springframework.http.HttpStatus;

import com.flowcollect.exception.base.BusinessException;
import com.flowcollect.exception.code.ErrorCode;

/**
 * Unexpected system condition that we explicitly detect.
 */
public class InternalException extends BusinessException {

    public InternalException(String message) {
        super(ErrorCode.INTERNAL_SERVER_ERROR, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
