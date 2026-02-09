package com.cashclarity.exception;

/**
 * Thrown when an organization ID is null or invalid.
 */
public class InvalidOrganizationIdException extends CashClarityException {

    public InvalidOrganizationIdException(Long organizationId) {
        super("Invalid organizationId: '" + organizationId + "'. The id must be a positive number.");
    }
}
