package com.cashclarity.exception.invoice;

import com.cashclarity.api.error.ErrorCode;
import com.cashclarity.exception.base.NotFoundException;

import java.util.UUID;

public class InvoiceNotFoundException extends NotFoundException {
    public InvoiceNotFoundException(String reason) {
        super(ErrorCode.INVOICE_NOT_FOUND, 
            reason);
    }
    public InvoiceNotFoundException(UUID id) {
        super(ErrorCode.INVOICE_NOT_FOUND, 
            "Invoice not found for id '" + id.toString() + "'.");
    }

}
