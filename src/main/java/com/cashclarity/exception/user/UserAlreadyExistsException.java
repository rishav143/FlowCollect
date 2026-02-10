package com.cashclarity.exception.user;

import com.cashclarity.exception.CashClarityException;

public class UserAlreadyExistsException extends CashClarityException {

    public UserAlreadyExistsException(String email, Long organizationId) {
        super("A user with email '" + email + "' already exists in organization '" + organizationId + "'.");
    }
}
