package com.cashclarity.exception;

/**
 * Thrown when an organization cannot be found for the given id.
 */
public class OrganizationNotFoundException extends CashClarityException {

    public OrganizationNotFoundException(Long organizationId) {
        super("Organization not found for id: '" + organizationId + "'.");
    }
}
