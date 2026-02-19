package com.cashclarity.exception.followup;

import java.util.UUID;

import com.cashclarity.api.error.ErrorCode;
import com.cashclarity.exception.base.ValidationException;
/**
 * Thrown when a follow-up field is invalid.
 */
public class InvalidFollowUpFieldException extends ValidationException {
    public InvalidFollowUpFieldException(String reason) {
        super(ErrorCode.INVALID_FOLLOW_UP_FIELD, 
            reason);
    }
    public InvalidFollowUpFieldException(UUID followUpId) {
        super(ErrorCode.INVALID_FOLLOW_UP_FIELD, 
            "Invalid follow-up id '" + followUpId.toString() + "'.");
    }
}