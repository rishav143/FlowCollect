package com.cashclarity.exception.organization;

import com.cashclarity.exception.CashClarityException;

/**
 * Thrown when an organization is already suspended.
 */
public class OrganizationAlreadySuspendedException extends CashClarityException {

    public OrganizationAlreadySuspendedException(Long organizationId) {
        super("Organization with id '" + organizationId + "' is already suspended.");
    }
}
