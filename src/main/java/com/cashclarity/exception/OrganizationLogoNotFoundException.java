package com.cashclarity.exception;

/**
 * Thrown when an organization logo is not set or not found.
 */
public class OrganizationLogoNotFoundException extends CashClarityException {

    public OrganizationLogoNotFoundException(Long organizationId) {
        super("Logo not found for organization id: '" + organizationId + "'.");
    }
}
