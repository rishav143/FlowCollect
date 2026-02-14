package com.cashclarity.exception.user;

import com.cashclarity.exception.CashClarityException;

public class UserNotFoundException extends CashClarityException {

    public UserNotFoundException(Long userId, Long organizationId) {
        super("User not found for id '" + userId + "' in organization '" + organizationId + "'.");
    }

    public UserNotFoundException(Long userId) {
        super("User not found for id '" + userId + "'.");
    }
}
