package com.cashclarity.exception.user;

import com.cashclarity.exception.CashClarityException;

public class InvalidUserFieldException extends CashClarityException {

    public InvalidUserFieldException(String field, String reason) {
        super("Invalid user field '" + field + "': " + reason);
    }
}
