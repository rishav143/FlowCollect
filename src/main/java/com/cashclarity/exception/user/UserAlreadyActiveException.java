package com.cashclarity.exception.user;

import com.cashclarity.exception.CashClarityException;

public class UserAlreadyActiveException extends CashClarityException {

    public UserAlreadyActiveException(Long userId, Long organizationId) {
        super("User with id '" + userId + "' is already active in organization '" + organizationId + "'.");
    }
}
