package com.cashclarity.exception.invoice;

import java.util.UUID;

import com.cashclarity.api.error.ErrorCode;
import com.cashclarity.exception.base.ValidationException;

public class InvalidInvoiceFieldException extends ValidationException {

    public InvalidInvoiceFieldException(String reason) {
        super(ErrorCode.INVALID_INVOICE_FIELD, 
            reason);
    }
    
    public InvalidInvoiceFieldException(UUID invoiceId) {
        super(ErrorCode.INVALID_INVOICE_FIELD, 
            "Invalid invoice id '" + invoiceId.toString() + "'.");
    }
}
