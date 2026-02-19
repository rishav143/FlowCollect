package com.cashclarity.exception.payment;

import java.util.UUID;

import com.cashclarity.api.error.ErrorCode;
import com.cashclarity.exception.base.ValidationException;

public class InvalidPaymentFieldException extends ValidationException{

    public InvalidPaymentFieldException(String reason) {
        super(ErrorCode.INVALID_PAYMENT_FIELD, 
            reason);
    }

    public InvalidPaymentFieldException(UUID paymentId) {
        super(ErrorCode.INVALID_PAYMENT_FIELD, 
            "Invalid payment id '" + paymentId.toString() + "'.");
    }
}
