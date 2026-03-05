package com.paidpeace.exception.http;

import org.springframework.http.HttpStatus;

import com.paidpeace.exception.base.BusinessException;
import com.paidpeace.exception.code.ErrorCode;

/**
 * Resource does not exist.
 */
public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message, HttpStatus.NOT_FOUND);
    }
}
