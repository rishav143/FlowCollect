package com.cashclarity.exception.organization;

import java.util.UUID;

import com.cashclarity.api.error.ErrorCode;
import com.cashclarity.exception.base.NotFoundException;

/**
 * Thrown when an organization cannot be found for the given id.
 */
public class OrganizationNotFoundException extends NotFoundException {

    public OrganizationNotFoundException(String reason) {
        super(ErrorCode.ORGANIZATION_NOT_FOUND, 
            reason);
    }

    public OrganizationNotFoundException(UUID organizationId) {
        super(ErrorCode.ORGANIZATION_NOT_FOUND, 
            "Organization not found for id: '" + organizationId.toString() + "'.");
    }
}
