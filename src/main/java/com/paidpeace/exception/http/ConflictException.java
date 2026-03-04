package com.paidpeace.exception.http;

import org.springframework.http.HttpStatus;

import com.paidpeace.exception.base.BusinessException;
import com.paidpeace.exception.code.ErrorCode;

/**
 * Conflict with current state.
 * Example: already exists, already active, duplicate email
 */
public class ConflictException extends BusinessException {

    public ConflictException(ErrorCode code, String message) {
        super(code, message, HttpStatus.CONFLICT);
    }
}
