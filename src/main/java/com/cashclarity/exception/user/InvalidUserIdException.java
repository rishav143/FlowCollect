package com.cashclarity.exception.user;

import com.cashclarity.exception.CashClarityException;

public class InvalidUserIdException extends CashClarityException {

    public InvalidUserIdException(Long userId) {
        super("Invalid userId: '" + userId + "'. The id must be a positive number.");
    }
}
