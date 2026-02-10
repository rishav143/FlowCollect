package com.cashclarity.exception.organization;

import com.cashclarity.exception.CashClarityException;

/**
 * Thrown when an organization is already archived/deleted.
 */
public class OrganizationAlreadyArchivedException extends CashClarityException {

    public OrganizationAlreadyArchivedException(Long organizationId) {
        super("Organization with id '" + organizationId + "' is already archived.");
    }
}
