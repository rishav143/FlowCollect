package com.cashclarity.exception.invoice;

import com.cashclarity.exception.CashClarityException;

public class InvoiceAlreadyExistsException extends CashClarityException {

    public InvoiceAlreadyExistsException(String invoiceNumber, Long organizationId) {
        super("An invoice with number '" + invoiceNumber + "' already exists in organization '" + organizationId + "'.");
    }
}
