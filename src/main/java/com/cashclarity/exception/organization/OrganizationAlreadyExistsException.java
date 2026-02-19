package com.cashclarity.exception.organization;

import java.util.UUID;

import com.cashclarity.api.error.ErrorCode;
import com.cashclarity.exception.base.ConflictException;

/**
 * Thrown when an organization cannot be created because another organization
 * already exists with the same email.
 */
public class OrganizationAlreadyExistsException extends ConflictException {

    public OrganizationAlreadyExistsException(String reason) {
        super(ErrorCode.ORGANIZATION_ALREADY_EXISTS, 
            reason);
    }

    public OrganizationAlreadyExistsException(UUID organizationId) {
        super(ErrorCode.ORGANIZATION_ALREADY_EXISTS, 
            "Organization with id '" + organizationId.toString() + "' already exists.");
    }
}
