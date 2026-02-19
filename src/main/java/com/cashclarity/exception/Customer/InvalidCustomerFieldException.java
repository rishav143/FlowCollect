package com.cashclarity.exception.customer;

import java.util.UUID;

import com.cashclarity.api.error.ErrorCode;
import com.cashclarity.exception.base.ValidationException;

public class InvalidCustomerFieldException extends ValidationException {
    public InvalidCustomerFieldException(String reason) {
        super(ErrorCode.INVALID_CUSTOMER_FIELD, 
            reason);
    }
    public InvalidCustomerFieldException(UUID customerId) {
        super(ErrorCode.INVALID_CUSTOMER_FIELD, 
            "Invalid customer id " + customerId.toString() + ".");
    }
}
