package com.cashclarity.exception.organization;

import java.util.UUID;

import com.cashclarity.api.error.ErrorCode;
import com.cashclarity.exception.base.ValidationException;

/**
 * Thrown when an organization field is present but invalid for update.
 */
public class InvalidOrganizationFieldException extends ValidationException {

    public InvalidOrganizationFieldException(String reason) {
        super(ErrorCode.INVALID_ORGANIZATION_FIELD, 
            reason);
    }

    public InvalidOrganizationFieldException(UUID organizationId) {
        super(ErrorCode.INVALID_ORGANIZATION_FIELD, 
            "Invalid organization id '" + organizationId.toString());
    }
}
