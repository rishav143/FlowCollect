package com.cashclarity.exception.payment;

import java.util.UUID;

import com.cashclarity.api.error.ErrorCode;
import com.cashclarity.exception.base.NotFoundException;

public class PaymentNotFoundException extends NotFoundException {
    public PaymentNotFoundException(String reason) {
        super(ErrorCode.PAYMENT_NOT_FOUND, 
            reason);
    }
    public PaymentNotFoundException(UUID paymentId) {
        super(ErrorCode.PAYMENT_NOT_FOUND, 
            "Payment not found with id: '" + paymentId.toString() + "'.");
    }
}
