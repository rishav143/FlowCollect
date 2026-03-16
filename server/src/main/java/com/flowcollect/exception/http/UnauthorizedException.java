package com.flowcollect.exception.http;

import org.springframework.http.HttpStatus;

import com.flowcollect.exception.base.BusinessException;
import com.flowcollect.exception.code.ErrorCode;

/**
 * Authentication required or token invalid.
 */
public class UnauthorizedException extends BusinessException {

    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message, HttpStatus.UNAUTHORIZED);
    }
}
