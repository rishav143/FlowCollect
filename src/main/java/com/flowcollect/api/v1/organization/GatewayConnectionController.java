package com.flowcollect.api.v1.organization;

import com.flowcollect.api.v1.organization.dto.GatewayConnectionResponse;
import com.flowcollect.api.v1.organization.dto.RazorpayConnectRequest;
import com.flowcollect.application.organization.GatewayConnectionService;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.domain.invoice.paymentlink.PaymentGateway;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.organization.gateway.OrganizationGatewayConfig;
import com.flowcollect.security.RequireRole;
import com.flowcollect.domain.user.UserRole;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages payment gateway connections for an organization.
 *
 * Base path: /api/v1/organizations/{organizationId}/gateways
 *
 * Stripe Connect flow:
 *   1. GET  /stripe/authorize-url  → frontend opens this URL
 *   2. User authorizes on Stripe
 *   3. POST /stripe/connect        → backend exchanges auth code for account ID
 *   4. DELETE /stripe              → disconnect
 *
 * Razorpay flow:
 *   1. POST /razorpay/connect      → save encrypted API keys
 *   2. DELETE /razorpay            → disconnect + clear keys
 */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/gateways")
public class GatewayConnectionController {

    private final GatewayConnectionService connectionService;
    private final OrganizationService organizationService;

    @Value("${app.base-url}")
    private String appBaseUrl;

    public GatewayConnectionController(
            GatewayConnectionService connectionService,
            OrganizationService organizationService
    ) {
        this.connectionService = connectionService;
        this.organizationService = organizationService;
    }

    // -----------------------------------------------------------------------
    // Read
    // -----------------------------------------------------------------------

    /**
     * Lists all gateway connections for this organization.
     * Returns connection metadata only — never raw credentials.
     */
    @GetMapping
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<List<GatewayConnectionResponse>> listConnections(
            @PathVariable UUID organizationId
    ) {
        List<OrganizationGatewayConfig> configs = connectionService.listConfigs(organizationId);
        List<GatewayConnectionResponse> responses = configs.stream()
                .map(GatewayConnectionResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    // -----------------------------------------------------------------------
    // Stripe Connect
    // -----------------------------------------------------------------------

    /**
     * Returns the Stripe OAuth URL for the organization to authorize FlowCollect.
     * The frontend should open or redirect to this URL.
     */
    @GetMapping("/stripe/authorize-url")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<Map<String, String>> getStripeAuthorizeUrl(
            @PathVariable UUID organizationId
    ) {
        String callbackUrl = appBaseUrl + "/api/v1/organizations/" + organizationId + "/gateways/stripe/connect";
        String url = connectionService.buildStripeConnectUrl(organizationId, callbackUrl);
        return ResponseEntity.ok(Map.of("authorizeUrl", url));
    }

    /**
     * Handles the Stripe OAuth callback.
     * Receives the authorization code, exchanges it for a connected account ID,
     * and persists the connection.
     *
     * Called by the frontend after Stripe redirects back with ?code=...&state=...
     */
    @PostMapping("/stripe/connect")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<GatewayConnectionResponse> connectStripe(
            @PathVariable UUID organizationId,
            @RequestParam String code
    ) {
        Organization organization = organizationService.getById(organizationId);
        OrganizationGatewayConfig config = connectionService.connectStripe(organization, code);
        return ResponseEntity.ok(GatewayConnectionResponse.from(config));
    }

    /**
     * Disconnects this organization's Stripe account.
     * Existing active payment links will stop working.
     */
    @DeleteMapping("/stripe")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<GatewayConnectionResponse> disconnectStripe(
            @PathVariable UUID organizationId
    ) {
        Organization organization = organizationService.getById(organizationId);
        OrganizationGatewayConfig config = connectionService.disconnect(organization, PaymentGateway.STRIPE);
        return ResponseEntity.ok(GatewayConnectionResponse.from(config));
    }

    // -----------------------------------------------------------------------
    // Razorpay (manual key entry)
    // -----------------------------------------------------------------------

    /**
     * Saves encrypted Razorpay credentials for this organization.
     * The keys are validated against the Razorpay API before being stored.
     *
     * Idempotent: calling again with new keys updates the existing connection.
     */
    @PostMapping("/razorpay/connect")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<GatewayConnectionResponse> connectRazorpay(
            @PathVariable UUID organizationId,
            @Valid @RequestBody RazorpayConnectRequest request
    ) {
        Organization organization = organizationService.getById(organizationId);
        OrganizationGatewayConfig config = connectionService.connectRazorpay(
                organization,
                request.getKeyId(),
                request.getKeySecret(),
                request.getWebhookSecret()
        );
        return ResponseEntity.ok(GatewayConnectionResponse.from(config));
    }

    /**
     * Disconnects Razorpay and clears all stored credentials.
     */
    @DeleteMapping("/razorpay")
    @RequireRole(UserRole.ADMIN)
    public ResponseEntity<GatewayConnectionResponse> disconnectRazorpay(
            @PathVariable UUID organizationId
    ) {
        Organization organization = organizationService.getById(organizationId);
        OrganizationGatewayConfig config = connectionService.disconnect(organization, PaymentGateway.RAZORPAY);
        return ResponseEntity.ok(GatewayConnectionResponse.from(config));
    }
}
