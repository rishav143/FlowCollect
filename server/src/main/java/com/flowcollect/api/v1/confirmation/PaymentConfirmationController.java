package com.flowcollect.api.v1.confirmation;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.flowcollect.api.v1.confirmation.dto.PaymentConfirmationResponse;
import com.flowcollect.api.v1.confirmation.dto.ReviewConfirmationRequest;
import com.flowcollect.application.confirmation.PaymentConfirmationService;
import com.flowcollect.domain.confirmation.PaymentConfirmation;
import com.flowcollect.domain.confirmation.PaymentConfirmationStatus;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.security.RequireRole;

import jakarta.validation.Valid;

/**
 * Authenticated endpoints for business users to manage payment confirmations
 * submitted by their customers.
 *
 * <p>All routes are scoped to an organization and require ADMIN or STAFF role.
 */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/payment-confirmations")
@RequireRole({ UserRole.ADMIN, UserRole.STAFF })
public class PaymentConfirmationController {

    private final PaymentConfirmationService paymentConfirmationService;

    public PaymentConfirmationController(PaymentConfirmationService paymentConfirmationService) {
        this.paymentConfirmationService = paymentConfirmationService;
    }

    /**
     * Lists payment confirmations for the organization, with optional status filter.
     *
     * @param organizationId the organization
     * @param status         optional filter: PENDING_APPROVAL | APPROVED | REJECTED
     * @param pageable       pagination (default page=0, size=20)
     */
    @GetMapping
    public ResponseEntity<Page<PaymentConfirmationResponse>> list(
            @PathVariable UUID organizationId,
            @RequestParam(required = false) PaymentConfirmationStatus status,
            Pageable pageable
    ) {
        Page<PaymentConfirmation> page = paymentConfirmationService.listForOrganization(
                organizationId, status, pageable);
        return ResponseEntity.ok(page.map(PaymentConfirmationMapper::toResponse));
    }

    /**
     * Returns a single payment confirmation by ID.
     */
    @GetMapping("/{confirmationId}")
    public ResponseEntity<PaymentConfirmationResponse> getById(
            @PathVariable UUID organizationId,
            @PathVariable UUID confirmationId
    ) {
        PaymentConfirmation confirmation = paymentConfirmationService.getForOrganization(
                organizationId, confirmationId);
        return ResponseEntity.ok(PaymentConfirmationMapper.toResponse(confirmation));
    }

    /**
     * Approves a pending payment confirmation.
     *
     * <p>Records the claimed amount as a payment on the invoice. If the invoice becomes
     * fully paid, the confirmation link is automatically closed.
     *
     * @param confirmationId the confirmation to approve
     * @param request        optional note from the business user
     */
    @PostMapping("/{confirmationId}/approve")
    public ResponseEntity<PaymentConfirmationResponse> approve(
            @PathVariable UUID organizationId,
            @PathVariable UUID confirmationId,
            @Valid @RequestBody(required = false) ReviewConfirmationRequest request
    ) {
        PaymentConfirmation confirmation = paymentConfirmationService.approveByBusiness(
                organizationId, confirmationId, request);
        return ResponseEntity.ok(PaymentConfirmationMapper.toResponse(confirmation));
    }

    /**
     * Approves a partial payment claim and requests the outstanding balance from the
     * customer in one action — no modal, no user input required.
     *
     * <p>Records the claimed amount as a payment (same as {@code /approve}) and
     * automatically sends a system-generated installment request email to the customer
     * asking for the remaining balance by the original invoice due date.
     *
     * <p>Returns {@code 409 Conflict} when the claimed amount equals the remaining
     * invoice balance (use {@code /approve} for full-payment claims).
     *
     * @param confirmationId the confirmation to approve
     * @param request        optional note; {@code notifyCustomer} defaults to {@code true}
     */
    @PostMapping("/{confirmationId}/request-remaining")
    public ResponseEntity<PaymentConfirmationResponse> requestRemaining(
            @PathVariable UUID organizationId,
            @PathVariable UUID confirmationId,
            @Valid @RequestBody(required = false) ReviewConfirmationRequest request
    ) {
        PaymentConfirmation confirmation = paymentConfirmationService.requestRemainingByBusiness(
                organizationId, confirmationId, request);
        return ResponseEntity.ok(PaymentConfirmationMapper.toResponse(confirmation));
    }

    /**
     * Rejects a pending payment confirmation.
     *
     * <p>No payment is recorded. The confirmation link remains open so the customer
     * may resubmit with corrected information.
     *
     * @param confirmationId the confirmation to reject
     * @param request        optional note from the business user
     */
    @PostMapping("/{confirmationId}/reject")
    public ResponseEntity<PaymentConfirmationResponse> reject(
            @PathVariable UUID organizationId,
            @PathVariable UUID confirmationId,
            @Valid @RequestBody(required = false) ReviewConfirmationRequest request
    ) {
        PaymentConfirmation confirmation = paymentConfirmationService.rejectByBusiness(
                organizationId, confirmationId, request);
        return ResponseEntity.ok(PaymentConfirmationMapper.toResponse(confirmation));
    }
}
