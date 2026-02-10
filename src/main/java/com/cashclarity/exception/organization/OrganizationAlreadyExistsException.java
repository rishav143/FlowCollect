package com.cashclarity.exception.organization;

import com.cashclarity.exception.CashClarityException;

/**
 * Thrown when an organization cannot be created because another organization
 * already exists with the same email.
 */
public class OrganizationAlreadyExistsException extends CashClarityException {

    public OrganizationAlreadyExistsException(String email) {
        super("An organization with email '" + email + "' already exists.");
    }
}
