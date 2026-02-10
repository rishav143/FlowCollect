package com.cashclarity.exception.organization;

import com.cashclarity.exception.CashClarityException;

/**
 * Thrown when an organization field is present but invalid for update.
 */
public class InvalidOrganizationFieldException extends CashClarityException {

    public InvalidOrganizationFieldException(String field, String reason) {
        super("Invalid organization field '" + field + "': " + reason + ".");
    }
}
