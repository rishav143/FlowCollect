package com.flowcollect.application.organization;

import com.flowcollect.domain.invoice.paymentlink.PaymentGateway;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.organization.gateway.GatewayConnectionStatus;
import com.flowcollect.domain.organization.gateway.OrganizationGatewayConfig;
import com.flowcollect.exception.http.ConflictException;
import com.flowcollect.exception.http.InternalException;
import com.flowcollect.exception.http.NotFoundException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.config.StripeProperties;
import com.flowcollect.infrastructure.persistence.organization.OrganizationGatewayConfigRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.model.oauth.TokenResponse;
import com.stripe.net.OAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages each organization's connection to payment gateways.
 *
 * Stripe Connect (recommended):
 *   Organizations connect their own Stripe account via OAuth.
 *   All payments go directly to their linked bank account.
 *   FlowCollect never touches the money.
 *
 * Razorpay (manual key entry):
 *   Organizations paste their Razorpay Key ID + Secret into settings.
 *   Keys are encrypted at rest using AES-256-GCM before storage.
 */
@Service
public class GatewayConnectionService {

    private static final Logger log = LoggerFactory.getLogger(GatewayConnectionService.class);

    private final OrganizationGatewayConfigRepository configRepository;
    private final GatewayCredentialEncryption encryption;
    private final StripeProperties stripeProperties;

    public GatewayConnectionService(
            OrganizationGatewayConfigRepository configRepository,
            GatewayCredentialEncryption encryption,
            StripeProperties stripeProperties
    ) {
        this.configRepository = configRepository;
        this.encryption = encryption;
        this.stripeProperties = stripeProperties;
    }

    // -----------------------------------------------------------------------
    // Stripe Connect
    // -----------------------------------------------------------------------

    /**
     * Returns the Stripe OAuth authorization URL for an organization.
     *
     * The organization owner opens this URL in their browser, logs into Stripe,
     * and authorizes FlowCollect to act on their account.
     * Stripe then redirects to our callback URL with an authorization code.
     *
     * @param organizationId The organization initiating the connection.
     * @param redirectUri    The callback URL configured in Stripe Dashboard.
     * @return Stripe OAuth URL to redirect the user to.
     */
    public String buildStripeConnectUrl(UUID organizationId, String redirectUri) {
        if (stripeProperties.getConnectClientId() == null || stripeProperties.getConnectClientId().isBlank()) {
            throw new ValidationException("Stripe Connect is not configured on this platform");
        }

        // Encode organizationId in state so we know which org to link on callback
        String state = organizationId.toString();

        return "https://connect.stripe.com/oauth/authorize"
                + "?response_type=code"
                + "&client_id=" + stripeProperties.getConnectClientId()
                + "&scope=read_write"
                + "&redirect_uri=" + redirectUri
                + "&state=" + state;
    }

    /**
     * Handles the Stripe Connect OAuth callback.
     *
     * Exchanges the authorization code for a Stripe connected account ID,
     * then persists the connection for the organization.
     *
     * @param organizationId  Extracted from the OAuth state parameter.
     * @param authorizationCode The code returned by Stripe in the callback.
     * @param organization     The Organization entity (loaded by caller).
     */
    @Transactional
    public OrganizationGatewayConfig connectStripe(
            Organization organization,
            String authorizationCode
    ) {
        Stripe.apiKey = stripeProperties.getApiKey();

        TokenResponse token;
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("code", authorizationCode);
            params.put("grant_type", "authorization_code");
            token = OAuth.token(params, null);
        } catch (StripeException e) {
            throw new InternalException("Stripe OAuth token exchange failed: " + e.getMessage());
        }

        String stripeAccountId = token.getStripeUserId();
        if (stripeAccountId == null || stripeAccountId.isBlank()) {
            throw new InternalException("Stripe did not return a connected account ID");
        }

        // Ensure this Stripe account isn't already connected to a different organization
        configRepository.findByStripeAccountId(stripeAccountId).ifPresent(existing -> {
            if (!existing.getOrganization().getId().equals(organization.getId())) {
                throw new ConflictException(
                        "This Stripe account is already connected to another organization");
            }
        });

        // Fetch the connected account's display name to show in the UI
        String displayName = null;
        try {
            Account account = Account.retrieve(stripeAccountId);
            if (account.getBusinessProfile() != null && account.getBusinessProfile().getName() != null) {
                displayName = account.getBusinessProfile().getName();
            } else if (account.getEmail() != null) {
                displayName = account.getEmail();
            }
        } catch (StripeException e) {
            log.warn("Could not fetch Stripe account name for {}: {}", stripeAccountId, e.getMessage());
        }

        OrganizationGatewayConfig config = findOrCreate(organization, PaymentGateway.STRIPE);
        config.setStripeAccountId(stripeAccountId);
        config.setDisplayName(displayName);
        config.connect();

        OrganizationGatewayConfig saved = configRepository.save(config);
        log.info("Organization {} connected Stripe account {}", organization.getId(), stripeAccountId);
        return saved;
    }

    // -----------------------------------------------------------------------
    // Razorpay (manual key entry)
    // -----------------------------------------------------------------------

    /**
     * Saves encrypted Razorpay credentials for an organization.
     * Credentials are validated by making a lightweight Razorpay API call
     * before being stored to prevent saving invalid keys.
     *
     * @param organization   The Organization entity.
     * @param keyId          Razorpay Key ID (e.g. rzp_live_xxxxx).
     * @param keySecret      Razorpay Key Secret.
     * @param webhookSecret  Razorpay webhook secret (from Razorpay dashboard).
     */
    @Transactional
    public OrganizationGatewayConfig connectRazorpay(
            Organization organization,
            String keyId,
            String keySecret,
            String webhookSecret
    ) {
        if (keyId == null || keyId.isBlank()) throw new ValidationException("Razorpay key ID is required");
        if (keySecret == null || keySecret.isBlank()) throw new ValidationException("Razorpay key secret is required");
        if (webhookSecret == null || webhookSecret.isBlank()) throw new ValidationException("Razorpay webhook secret is required");

        validateRazorpayCredentials(keyId, keySecret);

        // Store the key ID prefix as a display name (e.g. "rzp_live_AbCdEf")
        // so the UI can show which account is connected without exposing the full key.
        String displayName = keyId.length() > 14 ? keyId.substring(0, 14) + "..." : keyId;

        OrganizationGatewayConfig config = findOrCreate(organization, PaymentGateway.RAZORPAY);
        config.setEncryptedKeyId(encryption.encrypt(keyId));
        config.setEncryptedKeySecret(encryption.encrypt(keySecret));
        config.setWebhookSecret(encryption.encrypt(webhookSecret));
        config.setDisplayName(displayName);
        config.connect();

        OrganizationGatewayConfig saved = configRepository.save(config);
        log.info("Organization {} connected Razorpay account", organization.getId());
        return saved;
    }

    // -----------------------------------------------------------------------
    // Disconnect
    // -----------------------------------------------------------------------

    /**
     * Disconnects an organization from a gateway and clears stored credentials.
     * Active payment links for this org will no longer work after disconnection.
     */
    @Transactional
    public OrganizationGatewayConfig disconnect(Organization organization, PaymentGateway gateway) {
        OrganizationGatewayConfig config = configRepository
                .findByOrganizationIdAndGateway(organization.getId(), gateway)
                .orElseThrow(() -> new NotFoundException(
                        "No " + gateway + " connection found for this organization"));

        config.disconnect();
        OrganizationGatewayConfig saved = configRepository.save(config);
        log.info("Organization {} disconnected from {}", organization.getId(), gateway);
        return saved;
    }

    // -----------------------------------------------------------------------
    // Queries
    // -----------------------------------------------------------------------

    /** Returns the CONNECTED gateway config for an org, or throws if not connected. */
    @Transactional(readOnly = true)
    public OrganizationGatewayConfig requireConnectedConfig(UUID organizationId, PaymentGateway gateway) {
        return configRepository
                .findByOrganizationIdAndGateway(organizationId, gateway)
                .filter(OrganizationGatewayConfig::isConnected)
                .orElseThrow(() -> new ValidationException(
                        "Organization has not connected a " + gateway + " account. " +
                        "Go to Settings → Payment Gateways to connect one."));
    }

    /** Returns all gateway configs for an organization (any status). */
    @Transactional(readOnly = true)
    public List<OrganizationGatewayConfig> listConfigs(UUID organizationId) {
        return configRepository.findByOrganizationId(organizationId);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private OrganizationGatewayConfig findOrCreate(Organization organization, PaymentGateway gateway) {
        return configRepository
                .findByOrganizationIdAndGateway(organization.getId(), gateway)
                .orElseGet(() -> {
                    OrganizationGatewayConfig c = new OrganizationGatewayConfig();
                    c.setOrganization(organization);
                    c.setGateway(gateway);
                    return c;
                });
    }

    /**
     * Validates Razorpay credentials by fetching the account details.
     * Throws ValidationException if the credentials are invalid.
     */
    private void validateRazorpayCredentials(String keyId, String keySecret) {
        try {
            com.razorpay.RazorpayClient client = new com.razorpay.RazorpayClient(keyId, keySecret);
            // Fetch account info — cheap call that confirms credentials are valid
            client.addons.fetchAll(new org.json.JSONObject());
        } catch (com.razorpay.RazorpayException e) {
            throw new ValidationException("Razorpay credentials are invalid: " + e.getMessage());
        }
    }
}
