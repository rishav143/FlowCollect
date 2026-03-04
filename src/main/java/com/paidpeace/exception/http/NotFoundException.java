package com.paidpeace.exception.http;

import org.springframework.http.HttpStatus;

import com.paidpeace.exception.base.BusinessException;
import com.paidpeace.exception.code.ErrorCode;

/**
 * Resource does not exist.
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(ErrorCode code, String message) {
        super(code, message, HttpStatus.NOT_FOUND);
    }

    public NotFoundException(String message) {
        super(message, HttpStatus.NOT_FOUND);
    }
}
