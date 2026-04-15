package com.flowcollect.exception.http;

import org.springframework.http.HttpStatus;

import com.flowcollect.exception.base.BusinessException;
import com.flowcollect.exception.code.ErrorCode;

/**
 * Thrown when an operation is blocked by the organization's current plan limits.
 * Maps to HTTP 402 Payment Required.
 */
public class PlanLimitException extends BusinessException {

    public PlanLimitException(String message) {
        super(ErrorCode.PLAN_LIMIT_EXCEEDED, message, HttpStatus.PAYMENT_REQUIRED);
    }
}
