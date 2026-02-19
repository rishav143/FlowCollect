package com.cashclarity.exception.followup;

import java.util.UUID;

import com.cashclarity.api.error.ErrorCode;
import com.cashclarity.exception.base.NotFoundException;

/**
 * Thrown when a follow-up does not exist.
 */
public class FollowUpNotFoundException extends NotFoundException {
    public FollowUpNotFoundException(UUID followUpId) {
        super(ErrorCode.FOLLOW_UP_NOT_FOUND, 
            "Follow-up not found with id: " + followUpId.toString());
    }
    public FollowUpNotFoundException(String reason) {
        super(ErrorCode.FOLLOW_UP_NOT_FOUND, 
            reason);
    }
}
