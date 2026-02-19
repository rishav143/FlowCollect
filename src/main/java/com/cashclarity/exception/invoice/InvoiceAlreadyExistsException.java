package com.cashclarity.exception.invoice;

import java.util.UUID;

import com.cashclarity.api.error.ErrorCode;
import com.cashclarity.exception.base.ConflictException;

public class InvoiceAlreadyExistsException extends ConflictException {

    public InvoiceAlreadyExistsException(String reason) {
        super(ErrorCode.INVOICE_ALREADY_EXISTS, 
            reason);
    }

    public InvoiceAlreadyExistsException(UUID invoiceId) {
        super(ErrorCode.INVOICE_ALREADY_EXISTS, 
            "Invoice with id '" + invoiceId.toString() + "' already exists.");
    }
}
