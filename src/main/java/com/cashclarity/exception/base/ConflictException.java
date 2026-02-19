package com.cashclarity.exception.base;

import org.springframework.http.HttpStatus;

import com.cashclarity.api.error.ErrorCode;

/**
 * Conflict with current state.
 * Example: already exists, already active, duplicate email
 */
public abstract class ConflictException extends AppException {

    protected ConflictException(ErrorCode code, String message) {
        super(code, message, HttpStatus.CONFLICT);
    }
}
