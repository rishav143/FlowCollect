package com.flowcollect.application.confirmation;

import java.math.BigDecimal;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowcollect.api.v1.confirmation.dto.CustomerPaymentSubmitRequest;
import com.flowcollect.api.v1.confirmation.dto.ReviewConfirmationRequest;
import com.flowcollect.application.invoice.InvoiceService;
import com.flowcollect.application.invoice.PaymentService;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.domain.confirmation.ConfirmationLink;
import com.flowcollect.domain.confirmation.PaymentConfirmation;
import com.flowcollect.domain.confirmation.PaymentConfirmationStatus;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.payment.PaymentMode;
import com.flowcollect.exception.http.ConflictException;
import com.flowcollect.exception.http.NotFoundException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.confirmation.PaymentConfirmationJpaRepository;

/**
 * Core service for the payment confirmation workflow.
 *
 * <h2>Customer side</h2>
 * <ol>
 *   <li>Customer opens the public confirmation URL and views the invoice summary.</li>
 *   <li>Customer submits a payment claim ({@link #submitByCustomer}).</li>
 *   <li>The business owner receives an email notification.</li>
 * </ol>
 *
 * <h2>Business side — three review actions</h2>
 * <ol>
 *   <li>{@link #approveByBusiness} — records the claimed amount and sends the appropriate
 *       email: full-payment confirmation when the invoice is now settled, or partial-payment
 *       acknowledgement when a balance remains.</li>
 *   <li>{@link #requestRemainingByBusiness} — records the claimed partial amount AND
 *       sends a system-generated installment request email to the customer asking for the
 *       outstanding balance. One-click, no user input required. Only valid when the claimed
 *       amount is less than the invoice remaining balance.</li>
 *   <li>{@link #rejectByBusiness} — no payment recorded; link stays OPEN; customer may
 *       resubmit.</li>
 * </ol>
 */
@Service
public class PaymentConfirmationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentConfirmationService.class);

    private final PaymentConfirmationJpaRepository confirmationRepository;
    private final ConfirmationLinkService confirmationLinkService;
    private final PaymentService paymentService;
    private final InvoiceService invoiceService;
    private final OrganizationService organizationService;
    private final BusinessNotificationService businessNotificationService;

    public PaymentConfirmationService(
            PaymentConfirmationJpaRepository confirmationRepository,
            ConfirmationLinkService confirmationLinkService,
            PaymentService paymentService,
            InvoiceService invoiceService,
            OrganizationService organizationService,
            BusinessNotificationService businessNotificationService
    ) {
        this.confirmationRepository = confirmationRepository;
        this.confirmationLinkService = confirmationLinkService;
        this.paymentService = paymentService;
        this.invoiceService = invoiceService;
        this.organizationService = organizationService;
        this.businessNotificationService = businessNotificationService;
    }

    // -----------------------------------------------------------------------
    // Customer-side
    // -----------------------------------------------------------------------

    /**
     * Processes a customer-submitted payment claim.
     *
     * <p>Guards enforced:
     * <ul>
     *   <li>Confirmation link must be OPEN.</li>
     *   <li>Invoice must be ISSUED or PARTIALLY_PAID.</li>
     *   <li>Only one PENDING_APPROVAL submission per link at a time.</li>
     *   <li>Amount claimed must be &gt; 0 and &le; the invoice's remaining balance.</li>
     * </ul>
     *
     * @param token   the public token from the confirmation link URL
     * @param request the customer's submission
     * @return the saved {@link PaymentConfirmation}
     */
    @Transactional
    public PaymentConfirmation submitByCustomer(String token, CustomerPaymentSubmitRequest request) {
        if (request == null) {
            throw new ValidationException("Submission request must not be null");
        }

        ConfirmationLink link = confirmationLinkService.getByToken(token);

        if (!link.isOpen()) {
            throw new ConflictException(
                "This invoice has already been settled. No further payment confirmations are accepted.");
        }

        Invoice invoice = link.getInvoice();

        if (!invoice.isIssued() && !invoice.isPartiallyPaid()) {
            throw new ConflictException(
                "Payment confirmations are only accepted for issued or partially paid invoices.");
        }

        if (confirmationRepository.existsByConfirmationLinkIdAndStatus(
                link.getId(), PaymentConfirmationStatus.PENDING_APPROVAL)) {
            throw new ConflictException(
                "A payment confirmation is already pending approval. " +
                "Please wait for the business to review your previous submission before resubmitting.");
        }

        BigDecimal amountClaimed = request.getAmountClaimed();
        if (amountClaimed == null || amountClaimed.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount claimed must be greater than zero");
        }
        BigDecimal remaining = invoice.getRemainingAmount();
        if (amountClaimed.compareTo(remaining) > 0) {
            throw new ValidationException(
                "Amount claimed (" + amountClaimed + ") exceeds the invoice remaining balance (" + remaining + "). " +
                "For a partial payment, enter an amount up to " + remaining + ".");
        }

        PaymentConfirmation confirmation = new PaymentConfirmation();
        confirmation.setConfirmationLink(link);
        confirmation.setAmountClaimed(amountClaimed);
        if (request.getCustomerNote() != null && !request.getCustomerNote().isBlank()) {
            confirmation.setCustomerNote(request.getCustomerNote().trim());
        }

        PaymentConfirmation saved = confirmationRepository.save(confirmation);

        // Fire-and-forget — failure must never roll back the customer's submission
        try {
            businessNotificationService.notifyPaymentSubmitted(invoice, saved);
        } catch (Exception ex) {
            log.warn("Business notification failed for confirmation [confirmationId={}]: {}",
                    saved.getId(), ex.getMessage());
        }

        log.info("Customer submitted payment confirmation [confirmationId={}, invoiceId={}, amount={}]",
                saved.getId(), invoice.getId(), amountClaimed);

        return saved;
    }

    // -----------------------------------------------------------------------
    // Business-side review actions
    // -----------------------------------------------------------------------

    /**
     * Business user approves a payment confirmation.
     *
     * <p>Records the claimed amount as a BANK_TRANSFER payment on the invoice.
     * Closes the confirmation link when the invoice is now fully paid.
     *
     * <p>Email sent to the customer (when {@code request.notifyCustomer = true}):
     * <ul>
     *   <li>Invoice becomes PAID → full-payment confirmed email.</li>
     *   <li>Invoice remains PARTIALLY_PAID → partial-payment approved email
     *       (balance acknowledged, no explicit installment request).</li>
     * </ul>
     *
     * @param organizationId the authenticated organization
     * @param confirmationId the confirmation to approve
     * @param request        optional note; {@code notifyCustomer} defaults to {@code true}
     * @return the approved {@link PaymentConfirmation}
     */
    @Transactional
    public PaymentConfirmation approveByBusiness(
            UUID organizationId,
            UUID confirmationId,
            ReviewConfirmationRequest request
    ) {
        PaymentConfirmation confirmation = requirePendingConfirmationForOrg(organizationId, confirmationId);

        String note = extractNote(request);
        confirmation.approve(note);
        confirmationRepository.save(confirmation);

        Invoice invoice = confirmation.getConfirmationLink().getInvoice();
        paymentService.recordGatewayPayment(
                invoice.getId(),
                confirmation.getAmountClaimed(),
                PaymentMode.BANK_TRANSFER,
                null,
                buildApprovalPaymentNotes(note)
        );

        // Reload to get the updated lifecycle status after the payment is recorded
        Invoice refreshed = invoiceService.getInvoiceById(invoice.getId());
        if (refreshed.isPaid()) {
            confirmationLinkService.closeForInvoice(invoice.getId());
        }

        if (request == null || request.isNotifyCustomer()) {
            try {
                if (refreshed.isPaid()) {
                    businessNotificationService.notifyCustomerPaymentConfirmedFull(refreshed, confirmation);
                } else {
                    businessNotificationService.notifyCustomerPartialPaymentApproved(refreshed, confirmation);
                }
            } catch (Exception ex) {
                log.warn("Customer approval notification failed for confirmation [confirmationId={}]: {}",
                        confirmationId, ex.getMessage());
            }
        }

        log.info("Business approved payment confirmation [confirmationId={}, invoiceId={}, amount={}, fullyPaid={}]",
                confirmationId, invoice.getId(), confirmation.getAmountClaimed(), refreshed.isPaid());

        return confirmation;
    }

    /**
     * Business user approves a <em>partial</em> payment claim and requests the remaining
     * balance from the customer in a single one-click action.
     *
     * <p>Behaviour:
     * <ul>
     *   <li>Validates that the claimed amount is strictly less than the invoice's current
     *       remaining balance — i.e. this is genuinely a partial payment. Throws
     *       {@link ConflictException} if the claim covers the full remaining amount
     *       (use {@link #approveByBusiness} in that case).</li>
     *   <li>Records the claimed amount as a BANK_TRANSFER payment (same as approve).</li>
     *   <li>Sends a system-generated installment request email to the customer asking for
     *       the outstanding balance by the original invoice due date. No user input needed.</li>
     * </ul>
     *
     * @param organizationId the authenticated organization
     * @param confirmationId the confirmation to approve
     * @param request        optional note; {@code notifyCustomer} is respected
     * @return the approved {@link PaymentConfirmation}
     * @throws ConflictException if the claimed amount equals the remaining balance
     *         (not a partial payment — use /approve instead)
     */
    @Transactional
    public PaymentConfirmation requestRemainingByBusiness(
            UUID organizationId,
            UUID confirmationId,
            ReviewConfirmationRequest request
    ) {
        PaymentConfirmation confirmation = requirePendingConfirmationForOrg(organizationId, confirmationId);

        Invoice invoice = confirmation.getConfirmationLink().getInvoice();
        BigDecimal remaining = invoice.getRemainingAmount();

        // Guard: must be a genuinely partial claim
        if (confirmation.getAmountClaimed().compareTo(remaining) >= 0) {
            throw new ConflictException(
                "The claimed amount covers the full remaining balance — use /approve to confirm this payment in full.");
        }

        // Apply new due date before recording payment so the email reflects the updated deadline
        if (request != null && request.getNewDueDate() != null) {
            invoice.setDueDate(request.getNewDueDate());
            invoiceService.save(invoice);
        }

        String note = extractNote(request);
        confirmation.requestRemaining(note);
        confirmationRepository.save(confirmation);

        paymentService.recordGatewayPayment(
                invoice.getId(),
                confirmation.getAmountClaimed(),
                PaymentMode.BANK_TRANSFER,
                null,
                "Partial payment approved; remaining balance requested from customer."
        );

        // Reload invoice so the remaining amount and due date in the notification are up to date
        Invoice refreshed = invoiceService.getInvoiceById(invoice.getId());

        // notifyCustomer defaults true — always send unless caller explicitly opts out
        if (request == null || request.isNotifyCustomer()) {
            try {
                businessNotificationService.notifyInstallmentRequest(refreshed, confirmation);
            } catch (Exception ex) {
                log.warn("Installment request notification failed for confirmation [confirmationId={}]: {}",
                        confirmationId, ex.getMessage());
            }
        }

        log.info("Business requested remaining balance after partial approval "
                + "[confirmationId={}, invoiceId={}, amountApproved={}, remainingAfter={}]",
                confirmationId, invoice.getId(), confirmation.getAmountClaimed(), refreshed.getRemainingAmount());

        return confirmation;
    }

    /**
     * Business user rejects a payment confirmation.
     *
     * <p>No payment is recorded. The confirmation link remains OPEN so the customer
     * may resubmit with corrected information.
     *
     * @param organizationId the authenticated organization
     * @param confirmationId the confirmation to reject
     * @param request        optional note; {@code notifyCustomer} defaults to {@code true}
     * @return the rejected {@link PaymentConfirmation}
     */
    @Transactional
    public PaymentConfirmation rejectByBusiness(
            UUID organizationId,
            UUID confirmationId,
            ReviewConfirmationRequest request
    ) {
        PaymentConfirmation confirmation = requirePendingConfirmationForOrg(organizationId, confirmationId);

        String note = extractNote(request);
        confirmation.reject(note);
        PaymentConfirmation saved = confirmationRepository.save(confirmation);

        Invoice invoice = confirmation.getConfirmationLink().getInvoice();
        if (request == null || request.isNotifyCustomer()) {
            try {
                businessNotificationService.notifyCustomerPaymentRejected(invoice, saved);
            } catch (Exception ex) {
                log.warn("Customer rejection notification failed for confirmation [confirmationId={}]: {}",
                        confirmationId, ex.getMessage());
            }
        }

        log.info("Business rejected payment confirmation [confirmationId={}, invoiceId={}]",
                confirmationId, invoice.getId());

        return saved;
    }

    // -----------------------------------------------------------------------
    // Read operations
    // -----------------------------------------------------------------------

    /**
     * Returns a paginated list of payment confirmations for the given organization,
     * optionally filtered by status.
     *
     * @param organizationId the organization whose confirmations to list
     * @param status         optional filter; if null, all statuses are returned
     * @param pageable       pagination settings
     */
    @Transactional(readOnly = true)
    public Page<PaymentConfirmation> listForOrganization(
            UUID organizationId,
            PaymentConfirmationStatus status,
            Pageable pageable
    ) {
        organizationService.getById(organizationId);
        if (status != null) {
            return confirmationRepository.findByConfirmationLinkInvoiceOrganizationIdAndStatus(
                    organizationId, status, pageable);
        }
        return confirmationRepository.findByConfirmationLinkInvoiceOrganizationId(organizationId, pageable);
    }

    /**
     * Returns a single payment confirmation by ID, scoped to the given organization.
     *
     * @throws NotFoundException if the confirmation does not exist or belongs to a different org
     */
    @Transactional(readOnly = true)
    public PaymentConfirmation getForOrganization(UUID organizationId, UUID confirmationId) {
        organizationService.getById(organizationId);
        PaymentConfirmation confirmation = confirmationRepository.findById(confirmationId)
                .orElseThrow(() -> new NotFoundException("Payment confirmation not found"));
        enforceOrganizationOwnership(organizationId, confirmation);
        return confirmation;
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private PaymentConfirmation requirePendingConfirmationForOrg(UUID organizationId, UUID confirmationId) {
        organizationService.getById(organizationId);
        PaymentConfirmation confirmation = confirmationRepository.findById(confirmationId)
                .orElseThrow(() -> new NotFoundException("Payment confirmation not found"));
        enforceOrganizationOwnership(organizationId, confirmation);
        if (!confirmation.isPendingApproval()) {
            throw new ConflictException(
                "Payment confirmation " + confirmationId +
                " is not in PENDING_APPROVAL state. Current status: " + confirmation.getStatus());
        }
        return confirmation;
    }

    private void enforceOrganizationOwnership(UUID organizationId, PaymentConfirmation confirmation) {
        UUID ownerOrgId = confirmation.getConfirmationLink().getInvoice().getOrganization().getId();
        if (!ownerOrgId.equals(organizationId)) {
            // Return 404 — do not reveal that the resource exists under another org
            throw new NotFoundException("Payment confirmation not found");
        }
    }

    private String extractNote(ReviewConfirmationRequest request) {
        if (request == null || request.getNote() == null) return null;
        String trimmed = request.getNote().trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private String buildApprovalPaymentNotes(String businessNote) {
        String base = "Customer self-reported payment (approved by business)";
        return businessNote != null ? base + ". Note: " + businessNote : base;
    }
}
