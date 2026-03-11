package com.paidpeace.application.invoice;

import java.util.List;
import java.util.UUID;

import com.paidpeace.api.v1.invoice.dto.InvoiceItemRequest;
import com.paidpeace.domain.invoice.Invoice;
import com.paidpeace.domain.invoice.followup.FollowUp;
import com.paidpeace.domain.invoice.payment.Payment;
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
            throw new ValidationException("invoiceId must not be null");
        }
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new NotFoundException("invoice not found with id " + invoiceId));
        if (invoice.getOrganization().isDeleted()) {
            throw new NotFoundException("organization is archived with id " + invoice.getOrganization().getId());
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
            throw new ValidationException("invoiceId must not be null");
        }
        if (organizationId == null) {
            throw new ValidationException("organizationId must not be null");
        }
        Invoice invoice = getInvoiceOrThrow(invoiceId, invoiceRepository);
        if (!invoice.getOrganization().getId().equals(organizationId)) {
            throw new ValidationException("invoice is not associated with organization with id " + organizationId);
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
            throw new ValidationException("paymentId must not be null");
        }
        if(invoiceId == null) {
            throw new ValidationException("invoiceId must not be null");
        }
        Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new NotFoundException("payment not found with id " + paymentId));
        validatePayment(invoiceId, payment);
        return payment;
    }

    public static void validatePayment(UUID invoiceId, Payment payment) {
        if (!payment.getInvoice().getId().equals(invoiceId)) {
            throw new ValidationException("payment does not belong to invoice with id " + invoiceId);
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
                .orElseThrow(() -> new NotFoundException("follow-up not found with id " + followUpId));
        validateFollowUp(invoiceId, followUp);
        return followUp;
    }

    public static void validateFollowUp
    (
        UUID invoiceId, 
        FollowUp followUp
    ) {
        if (!followUp.getInvoice().getId().equals(invoiceId)) {
            throw new ValidationException("follow-up must belong to invoice with id " + invoiceId);
        }
    }
}
