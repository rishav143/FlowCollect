package com.paidpeace.application.invoice;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.paidpeace.api.v1.invoice.dto.PaymentRequest;
import com.paidpeace.domain.invoice.payment.Payment;
import com.paidpeace.domain.invoice.payment.PaymentMode;
import com.paidpeace.exception.http.ValidationException;
import com.paidpeace.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.paidpeace.infrastructure.persistence.invoice.PaymentJpaRepository;

@Service
public class PaymentService {

    private final PaymentJpaRepository paymentRepository;
    private final InvoiceJpaRepository invoiceRepository;

    public PaymentService(PaymentJpaRepository paymentRepository, InvoiceJpaRepository invoiceRepository) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
    }
    
    /**
     * Creates a new payment for an invoice.
     * @param invoiceId The ID of the invoice to create a payment for.
     * @param paymentRequest The request body containing the payment details.
     * @return The created payment.
     */
    @Transactional
    public Payment createPayment
    (
        UUID invoiceId, 
        PaymentRequest paymentRequest
    ) {
        if(paymentRequest == null) {
            throw new ValidationException("Payment request cannot be null");
        }
        InvoiceUtil.getInvoiceOrThrow(invoiceId, invoiceRepository);

        Payment payment = new Payment();
        if(paymentRequest.getAmount() != null) {
            payment.setAmount(paymentRequest.getAmount());
        }
        if(paymentRequest.getMode() != null) {
            payment.setMode(paymentRequest.getMode());
        }
        if(paymentRequest.getReferenceId() != null) {
            payment.setReferenceId(paymentRequest.getReferenceId());
        }
        if(paymentRequest.getNotes() != null) {
            payment.setNotes(paymentRequest.getNotes());
        }
        return paymentRepository.save(payment);
    }

    /**
     * Gets a payment by its ID.
     */
    public Payment getPayment(UUID invoiceId, UUID paymentId) {
        return InvoiceUtil.getPaymentOrThrow(invoiceId, paymentId, paymentRepository);
    }

    /**
     * Gets all payments for an invoice.
     */
    public Page<Payment> getPayments(
        UUID invoiceId, 
        PaymentMode mode, 
        Instant paidAt, 
        Pageable pageable
    ) {
        // Validate the invoice ID.
        InvoiceUtil.getInvoiceOrThrow(invoiceId, invoiceRepository);

        // Create a specification for the payments.
        Specification<Payment> spec = (root, query, criteriaBuilder) -> {
            Predicate p = criteriaBuilder.equal(root.get("invoice"), invoiceId);
            if(mode != null) {
                p = criteriaBuilder.and(p, criteriaBuilder.equal(root.get("mode"), mode));
            }
            if(paidAt != null) {
                p = criteriaBuilder.and(p, criteriaBuilder.equal(root.get("paidAt"), paidAt));
            }
            return p;
        };
        return paymentRepository.findAll(spec, pageable);
    }

    /**
     * Updates a payment by its ID.
     */
    @Transactional
    public Payment updatePayment
    (
        UUID invoiceId, 
        UUID paymentId, 
        PaymentRequest paymentRequest
    ) {
        if(paymentRequest == null) {
            throw new ValidationException("Payment request must not be null");
        }
        InvoiceUtil.getInvoiceOrThrow(invoiceId, invoiceRepository);

        Payment payment  = InvoiceUtil.getPaymentOrThrow(invoiceId, paymentId, paymentRepository);
        if(paymentRequest.getAmount() != null) {
            payment.setAmount(paymentRequest.getAmount());
        }
        if(paymentRequest.getMode() != null) {
            payment.setMode(paymentRequest.getMode());
        }
        if(paymentRequest.getReferenceId() != null) {
            payment.setReferenceId(paymentRequest.getReferenceId());
        }
        if(paymentRequest.getNotes() != null) {
            payment.setNotes(paymentRequest.getNotes());
        }
        return paymentRepository.save(payment);
    }
}
