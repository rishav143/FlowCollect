package com.cashclarity.exception.user;

import com.cashclarity.exception.CashClarityException;

public class UserAlreadyInactiveException extends CashClarityException {

    public UserAlreadyInactiveException(Long userId, Long organizationId) {
        super("User with id '" + userId + "' is already inactive in organization '" + organizationId + "'.");
    }
}
