package com.cashclarity.exception.organization;

import com.cashclarity.exception.CashClarityException;

/**
 * Thrown when an organization logo is not set or not found.
 */
public class OrganizationLogoNotFoundException extends CashClarityException {

    public OrganizationLogoNotFoundException(Long organizationId) {
        super("Logo not found for organization id: '" + organizationId + "'.");
    }
}
