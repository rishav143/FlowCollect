package com.flowcollect.api.v1.billing;

import com.flowcollect.api.v1.billing.dto.CheckoutRequest;
import com.flowcollect.api.v1.billing.dto.CheckoutResponse;
import com.flowcollect.application.billing.DodoBillingService;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.domain.organization.OrgPlan;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.security.RequireRole;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Handles PaidPeace's own subscription billing (powered by Dodo Payments).
 *
 * Base path: /api/v1/organizations/{organizationId}/billing
 *
 * Upgrade flow:
 *   1. POST /checkout      → backend creates Dodo checkout session, returns URL
 *   2. Frontend redirects browser to the Dodo-hosted checkout page
 *   3. User pays on Dodo
 *   4. Dodo posts webhook to DodoWebhookController → plan activated
 *   5. Dodo redirects browser back to app.frontend-url/settings/billing?upgraded=true
 */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/billing")
public class BillingController {

    private final DodoBillingService    dodoBillingService;
    private final OrganizationService   organizationService;
    private final String                frontendUrl;

    public BillingController(
            DodoBillingService  dodoBillingService,
            OrganizationService organizationService,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.dodoBillingService  = dodoBillingService;
        this.organizationService = organizationService;
        this.frontendUrl         = frontendUrl;
    }

    /**
     * Creates a Dodo Payments checkout session for a plan upgrade.
     *
     * Only ADMIN users may initiate a checkout — staff cannot change billing.
     * Returns a URL the frontend should redirect the browser to.
     */
    @PostMapping("/checkout")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<CheckoutResponse> createCheckout(
            @PathVariable UUID organizationId,
            @Valid @RequestBody CheckoutRequest request
    ) {
        Organization org = organizationService.getAuthorizedById(organizationId);
        OrgPlan      plan = OrgPlan.valueOf(request.getPlan());

        String returnUrl = frontendUrl + "/settings/billing?upgraded=true";
        String cancelUrl = frontendUrl + "/settings/billing?cancelled=true";

        String checkoutUrl = dodoBillingService.createCheckoutSession(org, plan, returnUrl, cancelUrl);

        return ResponseEntity.ok(new CheckoutResponse(checkoutUrl));
    }
}
