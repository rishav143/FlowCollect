package com.cashclarity.exception.user;

import com.cashclarity.api.error.ErrorCode;
import com.cashclarity.exception.base.NotFoundException;
import java.util.UUID;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(String reason) {
        super(ErrorCode.USER_NOT_FOUND, 
            reason);
    }
    public UserNotFoundException(UUID userId) {
        super(ErrorCode.USER_NOT_FOUND, 
            "User not found for id '" + userId.toString() + "'.");
    }
}
