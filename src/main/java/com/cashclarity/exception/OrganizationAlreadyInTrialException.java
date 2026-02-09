package com.cashclarity.exception;

/**
 * Thrown when an organization is already in TRIAL status.
 */
public class OrganizationAlreadyInTrialException extends CashClarityException {

    public OrganizationAlreadyInTrialException(Long organizationId) {
        super("Organization with id '" + organizationId + "' is already in trial.");
    }
}
