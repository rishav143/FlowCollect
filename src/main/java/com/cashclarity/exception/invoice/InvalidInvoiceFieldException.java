package com.cashclarity.exception.invoice;

import com.cashclarity.exception.CashClarityException;

public class InvalidInvoiceFieldException extends CashClarityException {

    public InvalidInvoiceFieldException(String field, String reason) {
        super("Invalid invoice field '" + field + "': " + reason);
    }
}
