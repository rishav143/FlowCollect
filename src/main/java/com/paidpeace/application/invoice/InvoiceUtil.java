package com.paidpeace.application.invoice;

import java.util.List;
import java.util.UUID;

import com.paidpeace.api.v1.invoice.dto.InvoiceItemRequest;
import com.paidpeace.domain.invoice.Invoice;
import com.paidpeace.domain.invoice.followup.FollowUp;
import com.paidpeace.domain.invoice.payment.Payment;
import com.paidpeace.exception.code.InvoiceErrorCode;
import com.paidpeace.exception.code.OrganizationErrorCode;
import com.paidpeace.exception.http.NotFoundException;
import com.paidpeace.exception.http.ValidationException;
import com.paidpeace.infrastructure.persistence.invoice.FollowUpJpaRepository;
import com.paidpeace.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.paidpeace.infrastructure.persistence.invoice.PaymentJpaRepository;


public class InvoiceUtil {

    // invoice utility methods
    public static void addItems
    (
        Invoice invoice, 
        List<InvoiceItemRequest> items
    ) throws ValidationException {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (InvoiceItemRequest item : items) {
            invoice.addItem(item.getDescription(), item.getQuantity(), item.getUnitPrice());
        }
    }

    public static Invoice getInvoiceOrThrow
    (
        UUID invoiceId, 
        InvoiceJpaRepository invoiceRepository
    ) {
        if (invoiceId == null) {
            throw new ValidationException(InvoiceErrorCode.INVALID_INVOICE_FIELD,
                "invoiceId must not be null");
        }
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new NotFoundException(InvoiceErrorCode.INVOICE_NOT_FOUND,
                "invoice not found with id " + invoiceId));
        if (invoice.getOrganization().isDeleted()) {
            throw new NotFoundException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND,
                "organization is archived with id " + invoice.getOrganization().getId());
        }
        return invoice;
    }

    public static Invoice validateInvoiceWithOrganization
    (
        UUID invoiceId, 
        UUID organizationId, 
        InvoiceJpaRepository invoiceRepository
    ) {
        if (invoiceId == null) {
            throw new ValidationException(InvoiceErrorCode.INVALID_INVOICE_FIELD,
                "invoiceId must not be null");
        }
        if (organizationId == null) {
            throw new ValidationException(InvoiceErrorCode.INVALID_INVOICE_FIELD,
                "organizationId must not be null");
        }
        Invoice invoice = getInvoiceOrThrow(invoiceId, invoiceRepository);
        if (invoice.getOrganization().getId() != organizationId) {
            throw new ValidationException(InvoiceErrorCode.INVALID_INVOICE_FIELD,
                "invoice is not associated with organization with id " + organizationId);
        }
        return invoice;
    }

    // payment utility methods
    public static Payment getPaymentOrThrow
    (
        UUID invoiceId, 
        UUID paymentId, 
        PaymentJpaRepository paymentRepository
    ) throws ValidationException, NotFoundException {
        if(paymentId == null) {
            throw new ValidationException(InvoiceErrorCode.INVALID_PAYMENT_FIELD,
                "Payment ID must not be null");
        }
        if(invoiceId == null) {
            throw new ValidationException(InvoiceErrorCode.INVALID_INVOICE_FIELD,
                "Invoice ID must not be null");
        }
        Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new NotFoundException(InvoiceErrorCode.PAYMENT_NOT_FOUND,
            "Payment not found with ID: " + paymentId));
        validatePayment(invoiceId, payment);
        return payment;
    }

    public static void validatePayment(UUID invoiceId, Payment payment) {
        if(payment.getInvoice().getId() != invoiceId) {
            throw new ValidationException(InvoiceErrorCode.INVALID_PAYMENT_FIELD,
                "Payment does not belong to invoice with ID: " + invoiceId);
        }
    }

    // follow-up utility methods
    public static FollowUp getFollowUpOrThrow
    (
        UUID invoiceId, 
        UUID followUpId,
        FollowUpJpaRepository followUpRepository
    ) {
        FollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() -> new NotFoundException(InvoiceErrorCode.FOLLOW_UP_NOT_FOUND,
                    "Follow-up not found with ID: " + followUpId));
        validateFollowUp(invoiceId, followUp);
        return followUp;
    }

    public static void validateFollowUp
    (
        UUID invoiceId, 
        FollowUp followUp
    ) {
        if (followUp.getInvoice().getId() != invoiceId) {
            throw new ValidationException(InvoiceErrorCode.INVALID_FOLLOW_UP_FIELD,
                "Follow-up must belong to invoice with ID: " + invoiceId);
        }
    }
}
