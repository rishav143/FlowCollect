package com.cashclarity.exception.user;

import java.util.UUID;

import com.cashclarity.api.error.ErrorCode;
import com.cashclarity.exception.base.ConflictException;

public class UserAlreadyExistsException extends ConflictException {
    public UserAlreadyExistsException(String reason) {
        super(ErrorCode.USER_ALREADY_EXISTS, 
            reason);
    }
    public UserAlreadyExistsException(UUID userId) {
        super(ErrorCode.USER_ALREADY_EXISTS, 
            "User with id '" + userId.toString() + "' already exists.");
    }
}
