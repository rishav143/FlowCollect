package com.paidpeace.exception.code;

public enum InvoiceErrorCode implements ErrorCode {
    INVOICE_NOT_FOUND,
    INVOICE_ALREADY_EXISTS,
    INVALID_INVOICE_FIELD,

    // -------- PAYMENT --------
    PAYMENT_NOT_FOUND,
    PAYMENT_ALREADY_EXISTS,
    INVALID_PAYMENT_FIELD,

    // -------- FOLLOW_UP --------
    FOLLOW_UP_NOT_FOUND,
    FOLLOW_UP_ALREADY_EXISTS,
    INVALID_FOLLOW_UP_FIELD
}
