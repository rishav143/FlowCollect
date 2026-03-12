package com.flowcollect.api.v1.invoice;

import com.flowcollect.api.v1.invoice.dto.PaymentResponse;
import com.flowcollect.domain.invoice.payment.Payment;

public class PaymentMapper {
    public static PaymentResponse toResponse(Payment payment) {
        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setInvoiceId(payment.getInvoice().getId());
        response.setAmount(payment.getAmount());
        response.setMode(payment.getMode());
        response.setReferenceId(payment.getReferenceId());
        response.setNotes(payment.getNotes());
        response.setPaidAt(payment.getPaidAt());
        response.setCreatedAt(payment.getCreatedAt());
        return response;
    }
}
