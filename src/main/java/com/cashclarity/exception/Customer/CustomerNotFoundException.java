package com.cashclarity.exception.Customer;

import com.cashclarity.exception.CashClarityException;

public class CustomerNotFoundException extends CashClarityException {
    public CustomerNotFoundException(Long customerId) {
        super("Customer not found for id '" + customerId + "'.");
    }

}
