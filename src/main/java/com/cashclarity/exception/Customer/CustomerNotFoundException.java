package com.cashclarity.exception.customer;

import com.cashclarity.api.error.ErrorCode;
import com.cashclarity.exception.base.NotFoundException;
import java.util.UUID;

/**
 * Thrown when a customer does not exist.
 */
public class CustomerNotFoundException extends NotFoundException {

    public CustomerNotFoundException(String message) {
        super(ErrorCode.CUSTOMER_NOT_FOUND, message);
    }

    public CustomerNotFoundException(UUID customerId) {
        super(
            ErrorCode.CUSTOMER_NOT_FOUND,
            "Customer not found with id: " + customerId.toString()
        );
    }
}
