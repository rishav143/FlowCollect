package com.cashclarity.application.invoice;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import org.springframework.stereotype.Service;

import com.cashclarity.api.v1.invoice.dto.PaymentRequest;
import com.cashclarity.domain.invoice.payment.Payment;
import com.cashclarity.domain.invoice.payment.PaymentMode;
import com.cashclarity.exception.invoice.InvalidInvoiceFieldException;
import com.cashclarity.exception.payment.InvalidPaymentFieldException;
import com.cashclarity.exception.payment.PaymentNotFoundException;
import com.cashclarity.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.cashclarity.infrastructure.persistence.invoice.PaymentJpaRepository;

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
    public Payment createPayment(UUID invoiceId, PaymentRequest paymentRequest) {
        if(paymentRequest == null) {
            throw new InvalidPaymentFieldException("Payment request cannot be null");
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
     * @param invoiceId The ID of the invoice to get a payment for.
     * @param paymentId The ID of the payment to get.
     * @return The payment.
     */
    public Payment getPayment(UUID invoiceId, UUID paymentId) {
        return getPaymentOrThrow(invoiceId, paymentId);
    }

    /**
     * Gets all payments for an invoice.
     * @param invoiceId The ID of the invoice to get payments for.
     * @param mode The mode of the payments to get.
     * @param paidAt The paid at date of the payments to get.
     * @param pageable The pageable object to get the payments.
     * @return The payments.
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
     * @param invoiceId The ID of the invoice to update the payment for.
     * @param paymentId The ID of the payment to change the fields of.
     * @param paymentRequest The request body containing the payment update request.
     * @return The payment with the updated fields.
     */
    public Payment updatePayment(UUID invoiceId, UUID paymentId, PaymentRequest paymentRequest) {
        if(paymentRequest == null) {
            throw new InvalidPaymentFieldException("Payment request cannot be null");
        }
        InvoiceUtil.getInvoiceOrThrow(invoiceId, invoiceRepository);

        Payment payment  = getPaymentOrThrow(invoiceId, paymentId);
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

    //=====================
    // Payment Internal Service methods
    //=====================

    private Payment getPaymentOrThrow(UUID invoiceId, UUID paymentId) {
        if(paymentId == null) {
            throw new InvalidPaymentFieldException("paymentId must not be null");
        }
        if(invoiceId == null) {
            throw new InvalidInvoiceFieldException("invoiceId must not be null");
        }
        Payment payment = paymentRepository.findById(paymentId)
        .orElseThrow(() -> new PaymentNotFoundException(paymentId));
        validatePayment(invoiceId, payment);
        return payment;
    }

    private void validatePayment(UUID invoiceId, Payment payment) {
        if(payment.getInvoice().getId() != invoiceId) {
            throw new InvalidPaymentFieldException("payment does not belong to invoice");
        }
    }
}
