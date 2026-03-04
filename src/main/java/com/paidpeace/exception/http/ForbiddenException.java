package com.paidpeace.exception.http;

import org.springframework.http.HttpStatus;

import com.paidpeace.exception.base.BusinessException;
import com.paidpeace.exception.code.ErrorCode;

/**
 * Authenticated but not allowed to perform action.
 */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(ErrorCode code, String message) {
        super(code, message, HttpStatus.FORBIDDEN);
    }
}
