package com.flowcollect.application.invoice;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.flowcollect.api.v1.invoice.dto.PaymentRequest;
import com.flowcollect.application.confirmation.ConfirmationLinkService;
import com.flowcollect.domain.confirmation.PaymentConfirmation;
import com.flowcollect.domain.confirmation.PaymentConfirmationStatus;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.LifeCycleStatus;
import com.flowcollect.domain.invoice.payment.Payment;
import com.flowcollect.domain.invoice.payment.PaymentMode;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.confirmation.PaymentConfirmationJpaRepository;
import com.flowcollect.infrastructure.persistence.invoice.FollowUpJpaRepository;
import com.flowcollect.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.flowcollect.infrastructure.persistence.invoice.PaymentJpaRepository;

@Service
public class PaymentService {

    private final PaymentJpaRepository paymentRepository;
    private final InvoiceJpaRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final FollowUpJpaRepository followUpRepository;
    private final ConfirmationLinkService confirmationLinkService;
    private final PaymentConfirmationJpaRepository paymentConfirmationRepository;

    public PaymentService(
            PaymentJpaRepository paymentRepository,
            InvoiceJpaRepository invoiceRepository,
            InvoiceService invoiceService,
            FollowUpJpaRepository followUpRepository,
            ConfirmationLinkService confirmationLinkService,
            PaymentConfirmationJpaRepository paymentConfirmationRepository) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
        this.followUpRepository = followUpRepository;
        this.confirmationLinkService = confirmationLinkService;
        this.paymentConfirmationRepository = paymentConfirmationRepository;
    }

    // Create a new payment for an invoice.
    @Transactional
    public Payment createPayment
    (
        UUID organizationId,
        UUID invoiceId,
        PaymentRequest paymentRequest
    ) {
        if(paymentRequest == null) {
            throw new ValidationException("Payment request cannot be null");
        }
        Invoice invoice = invoiceService.getInvoiceById(organizationId, invoiceId);

        if (invoice.getLifeCycleStatus() == LifeCycleStatus.DRAFT) {
            throw new ValidationException("Cannot record payment on a draft invoice — issue it first");
        }
        if (invoice.getLifeCycleStatus() == LifeCycleStatus.CANCELLED) {
            throw new ValidationException("Cannot record payment on a cancelled invoice");
        }
        if (invoice.getLifeCycleStatus() == LifeCycleStatus.PAID) {
            throw new ValidationException("Invoice is already fully paid");
        }

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        if(paymentRequest.getAmount() == null || paymentRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount must be greater than zero");
        }
        payment.setAmount(paymentRequest.getAmount());
        if(paymentRequest.getMode() == null) {
            throw new ValidationException("Payment mode is required");
        }
        payment.setMode(paymentRequest.getMode());
        if(paymentRequest.getReferenceId() != null) {
            payment.setReferenceId(paymentRequest.getReferenceId());
        }
        if(paymentRequest.getNotes() != null) {
            payment.setNotes(paymentRequest.getNotes());
        }

        Payment savedPayment = paymentRepository.save(payment);
        tagAutoRecovery(savedPayment, invoiceId);
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

        // If the invoice reverted to an unpaid state, reopen the confirmation link so
        // the customer can resubmit via the same URL that was already sent to them.
        if (invoice.getLifeCycleStatus() != LifeCycleStatus.PAID) {
            confirmationLinkService.reopenForInvoice(invoiceId);
        }
    }

    /**
     * Internal method for recording a payment received through a payment gateway webhook.
     * Bypasses the API DTO layer so the application layer stays clean.
     * Callers (PaymentLinkService) are responsible for idempotency checks before calling this.
     */
    @Transactional
    public Payment recordGatewayPayment(
            UUID invoiceId,
            BigDecimal amount,
            PaymentMode mode,
            String referenceId,
            String notes
    ) {
        Invoice invoice = invoiceService.getInvoiceById(invoiceId);

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(amount);
        payment.setMode(mode != null ? mode : PaymentMode.CARD);
        if (referenceId != null) payment.setReferenceId(referenceId);
        if (notes != null) payment.setNotes(notes);

        Payment saved = paymentRepository.save(payment);
        tagAutoRecovery(saved, invoiceId);
        updateInvoiceStatus(invoiceId);
        return saved;
    }

    /**
     * If a SENT AUTO follow-up exists for this invoice, tag the payment with the
     * rule that drove recovery. Called after both API and gateway payment paths.
     */
    private void tagAutoRecovery(Payment payment, UUID invoiceId) {
        followUpRepository.findSentAutoFollowUpsByInvoiceId(invoiceId)
                .stream()
                .findFirst()
                .map(followUp -> followUp.getReminderRule() != null ? followUp.getReminderRule().getId() : null)
                .ifPresent(payment::setRecoveredByAutoRuleId);
        // Persist the tag (payment is already managed within the same transaction)
        paymentRepository.save(payment);
    }

    // Get a payment by its ID.
    public Payment getPayment(UUID organizationId, UUID invoiceId, UUID paymentId) {
        invoiceService.getInvoiceById(organizationId, invoiceId);
        return InvoiceUtil.getPaymentOrThrow(invoiceId, paymentId, paymentRepository);
    }

    // Get all payments for an invoice.
    public Page<Payment> getPayments(
        UUID organizationId,
        UUID invoiceId,
        PaymentMode mode,
        LocalDate paidOn,
        Pageable pageable
    ) {
        invoiceService.getInvoiceById(organizationId, invoiceId);

        Specification<Payment> spec = (root, query, cb) -> {
            Predicate p = cb.equal(root.get("invoice").get("id"), invoiceId);
            if (mode != null) {
                p = cb.and(p, cb.equal(root.get("mode"), mode));
            }
            if (paidOn != null) {
                var start = paidOn.atStartOfDay().toInstant(ZoneOffset.UTC);
                var end = paidOn.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
                p = cb.and(p, cb.greaterThanOrEqualTo(root.get("paidAt"), start));
                p = cb.and(p, cb.lessThan(root.get("paidAt"), end));
            }
            return p;
        };
        return paymentRepository.findAll(spec, pageable);
    }

    // Update a payment by its ID.
    @Transactional
    public Payment updatePayment
    (
        UUID organizationId,
        UUID invoiceId,
        UUID paymentId,
        PaymentRequest paymentRequest
    ) {
        if(paymentRequest == null) {
            throw new ValidationException("Payment request must not be null");
        }
        invoiceService.getInvoiceById(organizationId, invoiceId);

        Payment payment  = InvoiceUtil.getPaymentOrThrow(invoiceId, paymentId, paymentRepository);
        if(paymentRequest.getAmount() != null) {
            if (paymentRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Amount must be greater than zero");
            }
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

    // Delete a payment by its ID and recalculate invoice status.
    // Also auto-rejects any PENDING_APPROVAL confirmations on this invoice's link so
    // the customer can resubmit via the same URL instead of seeing "already submitted".
    @Transactional
    public void deletePayment(UUID organizationId, UUID invoiceId, UUID paymentId) {
        invoiceService.getInvoiceById(organizationId, invoiceId);
        Payment payment = InvoiceUtil.getPaymentOrThrow(invoiceId, paymentId, paymentRepository);
        paymentRepository.delete(payment);
        updateInvoiceStatus(invoiceId);
        rejectStalePendingConfirmations(invoiceId);
    }

    // Auto-reject any PENDING_APPROVAL confirmations whose link belongs to this invoice.
    // Called after a payment is deleted so the customer can resubmit without hitting the
    // "already submitted" guard in submitByCustomer.
    private void rejectStalePendingConfirmations(UUID invoiceId) {
        confirmationLinkService.findByInvoiceId(invoiceId).ifPresent(link -> {
            List<PaymentConfirmation> pending = paymentConfirmationRepository
                    .findByConfirmationLinkIdAndStatus(link.getId(), PaymentConfirmationStatus.PENDING_APPROVAL);
            for (PaymentConfirmation c : pending) {
                c.reject("Payment record deleted by business — please resubmit.");
            }
            if (!pending.isEmpty()) {
                paymentConfirmationRepository.saveAll(pending);
            }
        });
    }
}
