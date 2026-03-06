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
import com.paidpeace.infrastructure.persistence.invoice.PaymentJpaRepository;

@Service
public class PaymentService {

    private final PaymentJpaRepository paymentRepository;
    private final InvoiceService invoiceService;

    public PaymentService(PaymentJpaRepository paymentRepository, InvoiceService invoiceService) {
        this.paymentRepository = paymentRepository;
        this.invoiceService = invoiceService;
    }
    
    // Create a new payment for an invoice.
    @Transactional
    public Payment createPayment
    (
        UUID invoiceId, 
        PaymentRequest paymentRequest
    ) {
        if(paymentRequest == null) {
            throw new ValidationException("Payment request cannot be null");
        }
        invoiceService.getInvoiceById(invoiceId);

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

    // Get a payment by its ID.
    public Payment getPayment(UUID invoiceId, UUID paymentId) {
        return InvoiceUtil.getPaymentOrThrow(invoiceId, paymentId, paymentRepository);
    }

    // Get all payments for an invoice.
    public Page<Payment> getPayments(
        UUID invoiceId, 
        PaymentMode mode, 
        Instant paidAt, 
        Pageable pageable
    ) {
        // Validate the invoice ID.
        invoiceService.getInvoiceById(invoiceId);

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

    // Update a payment by its ID.
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
        invoiceService.getInvoiceById(invoiceId);

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
