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
 * <h2>Business side</h2>
 * <ol>
 *   <li>Business owner reviews pending claims via the dashboard.</li>
 *   <li>Approves ({@link #approveByBusiness}) — records payment on invoice, closes link if fully paid.</li>
 *   <li>Or rejects ({@link #rejectByBusiness}) — link stays OPEN; customer may resubmit.</li>
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

    /**
     * Processes a customer-submitted payment claim.
     *
     * <p>Guards enforced:
     * <ul>
     *   <li>Confirmation link must be {@link com.flowcollect.domain.confirmation.ConfirmationLinkStatus#OPEN}.</li>
     *   <li>Invoice must be ISSUED or PARTIALLY_PAID (collectible).</li>
     *   <li>Only one {@link PaymentConfirmationStatus#PENDING_APPROVAL} submission per link at a time.</li>
     *   <li>Amount claimed must be > 0 and ≤ the invoice's remaining balance.</li>
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

        // Only one pending submission at a time prevents the business from being flooded
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

        // Fire-and-forget: notify business owner. Any failure must not roll back the
        // customer's submission — the confirmation is already persisted at this point.
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

    /**
     * Business user approves a payment confirmation.
     *
     * <p>Records the claimed amount as a {@link com.flowcollect.domain.invoice.payment.PaymentMode#BANK_TRANSFER}
     * payment on the invoice (the natural default for self-reported payments), then closes the
     * confirmation link if the invoice is now fully paid.
     *
     * @param organizationId the authenticated organization
     * @param confirmationId the confirmation to approve
     * @param request        optional note from the business user
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
        String paymentNotes = buildApprovalPaymentNotes(note);

        paymentService.recordGatewayPayment(
                invoice.getId(),
                confirmation.getAmountClaimed(),
                PaymentMode.BANK_TRANSFER,
                null,
                paymentNotes
        );

        // Reload the invoice to get the updated lifecycle status after payment is recorded
        Invoice refreshed = invoiceService.getInvoiceById(invoice.getId());
        if (refreshed.isPaid()) {
            confirmationLinkService.closeForInvoice(invoice.getId());
        }

        if (request != null && request.isNotifyCustomer()) {
            try {
                businessNotificationService.notifyCustomerApproved(refreshed, confirmation);
            } catch (Exception ex) {
                log.warn("Customer approval notification failed for confirmation [confirmationId={}]: {}",
                        confirmationId, ex.getMessage());
            }
        }

        log.info("Business approved payment confirmation [confirmationId={}, invoiceId={}, amount={}]",
                confirmationId, invoice.getId(), confirmation.getAmountClaimed());

        return confirmation;
    }

    /**
     * Business user rejects a payment confirmation.
     *
     * <p>The confirmation link remains {@link com.flowcollect.domain.confirmation.ConfirmationLinkStatus#OPEN}
     * so the customer may resubmit. No payment is recorded.
     *
     * @param organizationId the authenticated organization
     * @param confirmationId the confirmation to reject
     * @param request        optional note from the business user
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
        if (request != null && request.isNotifyCustomer()) {
            try {
                businessNotificationService.notifyCustomerRejected(invoice, saved);
            } catch (Exception ex) {
                log.warn("Customer rejection notification failed for confirmation [confirmationId={}]: {}",
                        confirmationId, ex.getMessage());
            }
        }

        log.info("Business rejected payment confirmation [confirmationId={}, invoiceId={}]",
                confirmationId, invoice.getId());

        return saved;
    }

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
