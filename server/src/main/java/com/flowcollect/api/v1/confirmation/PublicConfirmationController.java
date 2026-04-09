package com.flowcollect.api.v1.confirmation;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.flowcollect.api.v1.confirmation.dto.CustomerConfirmationView;
import com.flowcollect.api.v1.confirmation.dto.CustomerPaymentSubmitRequest;
import com.flowcollect.api.v1.confirmation.dto.PaymentConfirmationSubmitResponse;
import com.flowcollect.application.confirmation.ConfirmationLinkService;
import com.flowcollect.application.confirmation.PaymentConfirmationService;
import com.flowcollect.application.organization.OrgPaymentDetailsService;
import com.flowcollect.domain.confirmation.ConfirmationLink;
import com.flowcollect.domain.confirmation.PaymentConfirmation;
import com.flowcollect.domain.organization.OrgPaymentDetails;

import jakarta.validation.Valid;

/**
 * Public (unauthenticated) endpoints for the payment confirmation flow.
 *
 * <p>These endpoints are intentionally open — the token embedded in the URL acts as
 * a capability grant. No session or JWT is required.
 *
 * <p>Security considerations:
 * <ul>
 *   <li>Tokens are 128-bit random values — brute-force is infeasible.</li>
 *   <li>Only invoice-level summary data (no PII beyond the customer name) is exposed.</li>
 *   <li>The token never reveals internal IDs.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/public/confirmations")
public class PublicConfirmationController {

    private final ConfirmationLinkService confirmationLinkService;
    private final PaymentConfirmationService paymentConfirmationService;
    private final OrgPaymentDetailsService orgPaymentDetailsService;

    public PublicConfirmationController(
            ConfirmationLinkService confirmationLinkService,
            PaymentConfirmationService paymentConfirmationService,
            OrgPaymentDetailsService orgPaymentDetailsService
    ) {
        this.confirmationLinkService = confirmationLinkService;
        this.paymentConfirmationService = paymentConfirmationService;
        this.orgPaymentDetailsService = orgPaymentDetailsService;
    }

    /**
     * Returns a read-only summary of the invoice associated with the confirmation link.
     * Used by the frontend to populate the confirmation page before the customer submits.
     *
     * @param token the 32-char hex token from the public URL
     * @return invoice summary visible to the customer
     */
    @GetMapping("/{token}")
    public ResponseEntity<CustomerConfirmationView> getConfirmationView(@PathVariable String token) {
        ConfirmationLink link = confirmationLinkService.getByToken(token);
        OrgPaymentDetails paymentDetails = orgPaymentDetailsService
                .findByOrgId(link.getInvoice().getOrganization().getId())
                .orElse(null);
        return ResponseEntity.ok(PaymentConfirmationMapper.toCustomerView(link, paymentDetails));
    }

    /**
     * Customer submits a payment claim for the invoice.
     *
     * <p>Returns {@code 201 Created} on success. Returns {@code 409 Conflict} if a claim
     * is already pending approval or the invoice is no longer collectible.
     *
     * @param token   the 32-char hex token from the public URL
     * @param request the customer's payment claim
     * @return slim confirmation response with the new confirmation ID
     */
    @PostMapping("/{token}")
    public ResponseEntity<PaymentConfirmationSubmitResponse> submitPaymentClaim(
            @PathVariable String token,
            @Valid @RequestBody CustomerPaymentSubmitRequest request
    ) {
        PaymentConfirmation confirmation = paymentConfirmationService.submitByCustomer(token, request);
        return ResponseEntity.status(201).body(PaymentConfirmationMapper.toSubmitResponse(confirmation));
    }
}
