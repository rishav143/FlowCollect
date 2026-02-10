package com.cashclarity.exception.organization;

import com.cashclarity.exception.CashClarityException;

/**
 * Thrown when an organization is already in EXPIRED status.
 */
public class OrganizationAlreadyExpiredException extends CashClarityException {

    public OrganizationAlreadyExpiredException(Long organizationId) {
        super("Organization with id '" + organizationId + "' is already expired.");
    }
}
