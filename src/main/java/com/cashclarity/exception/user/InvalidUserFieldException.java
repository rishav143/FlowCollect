package com.cashclarity.exception.user;

import java.util.UUID;

import com.cashclarity.api.error.ErrorCode;
import com.cashclarity.exception.base.ValidationException;

public class InvalidUserFieldException extends ValidationException {

    public InvalidUserFieldException(String reason) {
        super(ErrorCode.INVALID_USER_FIELD, 
            reason);
    }
    public InvalidUserFieldException(UUID userId) {
        super(ErrorCode.INVALID_USER_FIELD, 
            "Invalid user id '" + userId.toString() + "'.");
    }
}
