package com.flowcollect.application.invoice;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.flowcollect.api.v1.invoice.dto.PaymentRequest;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.payment.Payment;
import com.flowcollect.domain.invoice.payment.PaymentMode;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.flowcollect.infrastructure.persistence.invoice.PaymentJpaRepository;

@Service
public class PaymentService {

    private final PaymentJpaRepository paymentRepository;
    private final InvoiceJpaRepository invoiceRepository;
    private final InvoiceService invoiceService;

    public PaymentService(PaymentJpaRepository paymentRepository, InvoiceJpaRepository invoiceRepository, InvoiceService invoiceService) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
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
        Invoice invoice = invoiceService.getInvoiceById(invoiceId);

        Payment payment = new Payment();
        payment.setInvoice(invoice);
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
        
        Payment savedPayment = paymentRepository.save(payment);
        updateInvoiceStatus(invoiceId);
        return savedPayment;
    }

    private void updateInvoiceStatus(UUID invoiceId) {
        Invoice invoice = invoiceService.getInvoiceById(invoiceId);
        List<Payment> payments = paymentRepository.findByInvoiceId(invoiceId);
        
        BigDecimal totalPaid = payments.stream()
                .map(Payment::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        invoice.updateLifeCycleStatus(totalPaid);
        invoiceRepository.save(invoice);
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
        
        Payment savedPayment = paymentRepository.save(payment);
        updateInvoiceStatus(invoiceId);
        return savedPayment;
    }
}
