package com.cashclarity.exception;

/**
 * Thrown when an organization is already active.
 */
public class OrganizationAlreadyActiveException extends CashClarityException {

    public OrganizationAlreadyActiveException(Long organizationId) {
        super("Organization with id '" + organizationId + "' is already active.");
    }
}
