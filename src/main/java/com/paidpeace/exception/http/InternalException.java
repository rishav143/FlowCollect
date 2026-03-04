package com.paidpeace.exception.http;

import org.springframework.http.HttpStatus;

import com.paidpeace.exception.base.BusinessException;
import com.paidpeace.exception.code.ErrorCode;

/**
 * Unexpected system condition that we explicitly detect.
 */
public class InternalException extends BusinessException {

    public InternalException(ErrorCode code, String message) {
        super(code, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
