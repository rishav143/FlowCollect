package com.flowcollect.api.v1.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowcollect.domain.organization.OrgPlan;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.infrastructure.persistence.organization.OrganizationJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

/**
 * Receives Dodo Payments subscription lifecycle webhooks.
 *
 * Dodo uses the Standard Webhooks specification — identical algorithm to Resend/svix
 * but with different header names:
 *
 *   Dodo headers  : webhook-id, webhook-timestamp, webhook-signature
 *   Resend headers: svix-id,    svix-timestamp,    svix-signature
 *
 * Signature algorithm (both identical):
 *   signed_content = "{webhook-id}.{webhook-timestamp}.{raw-body}"
 *   signature      = Base64(HMAC-SHA256(Base64Decode(secret), signed_content))
 *   header format  = "v1,{signature}"  (space-separated if multiple)
 *
 * Events handled:
 *   subscription.active    → plan activated / reactivated
 *   subscription.renewed   → auto-renewal succeeded, period extended
 *   subscription.cancelled → user cancelled; keep PRO until period ends
 *   subscription.expired   → period ended; downgrade to STARTER
 *   subscription.on_hold   → payment failed; suspend org
 *
 * Idempotency: subscription.active checks existing dodoSubscriptionId before writing.
 * All other events are safe to replay (they set deterministic state).
 *
 * This endpoint is exempt from JWT auth via JwtFilter.SKIP_PREFIXES (/api/v1/webhooks/).
 */
@RestController
public class DodoWebhookController {

    private static final Logger log = LoggerFactory.getLogger(DodoWebhookController.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final ObjectMapper                  objectMapper;
    private final OrganizationJpaRepository     organizationRepository;
    private final String                        webhookSecret;
    private final String                        proProductIdInr;
    private final String                        proProductIdUsd;

    public DodoWebhookController(
            ObjectMapper                objectMapper,
            OrganizationJpaRepository   organizationRepository,
            @Value("${dodo.webhook-secret:}")        String webhookSecret,
            @Value("${dodo.pro-product-id-inr:}")    String proProductIdInr,
            @Value("${dodo.pro-product-id-usd:}")    String proProductIdUsd
    ) {
        this.objectMapper           = objectMapper;
        this.organizationRepository = organizationRepository;
        this.webhookSecret          = webhookSecret;
        this.proProductIdInr        = proProductIdInr;
        this.proProductIdUsd        = proProductIdUsd;

        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("DODO_WEBHOOK_SECRET is not set — Dodo webhook signature verification will be SKIPPED (dev only)");
        }
    }

    // ------------------------------------------------------------------
    // Endpoint
    // ------------------------------------------------------------------

    @PostMapping("/api/v1/webhooks/dodo")
    @Transactional
    public ResponseEntity<Void> handleDodoWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "webhook-id",        required = false) String webhookId,
            @RequestHeader(value = "webhook-timestamp", required = false) String webhookTimestamp,
            @RequestHeader(value = "webhook-signature", required = false) String webhookSignature
    ) {
        if (!verifySignature(webhookId, webhookTimestamp, webhookSignature, payload)) {
            log.warn("Dodo webhook signature verification failed — rejecting");
            return ResponseEntity.status(401).build();
        }

        try {
            process(payload);
        } catch (Exception ex) {
            // Log but return 200 — Dodo will retry on non-2xx, and most errors here
            // are permanent (bad payload shape, unknown org) not transient.
            log.error("Error processing Dodo webhook", ex);
        }

        return ResponseEntity.ok().build();
    }

    // ------------------------------------------------------------------
    // Event dispatch
    // ------------------------------------------------------------------

    private void process(String payload) throws Exception {
        JsonNode root      = objectMapper.readTree(payload);
        String   eventType = root.path("type").asText("");
        JsonNode data      = root.path("data");

        log.info("Dodo webhook received: type={}", eventType);

        switch (eventType) {
            case "subscription.active"    -> handleActive(data);
            case "subscription.renewed"   -> handleRenewed(data);
            case "subscription.cancelled" -> handleCancelled(data);
            case "subscription.expired"   -> handleExpired(data);
            case "subscription.on_hold"   -> handleOnHold(data);
            default -> log.debug("Unhandled Dodo event type: {}", eventType);
        }
    }

    // ------------------------------------------------------------------
    // Handlers
    // ------------------------------------------------------------------

    private void handleActive(JsonNode data) {
        Organization org = resolveOrg(data);
        if (org == null) return;

        String  subscriptionId = data.path("subscription_id").asText(null);
        String  customerId     = data.path("customer").path("customer_id").asText(null);
        Instant periodEnd      = parseInstant(data.path("current_period_end").asText(null));
        OrgPlan plan           = resolvePlan(data.path("product_id").asText(""));

        // Idempotency: skip if we already activated this exact subscription
        if (subscriptionId != null && subscriptionId.equals(org.getDodoSubscriptionId())) {
            log.debug("subscription.active already applied for sub={} — skipping", subscriptionId);
            return;
        }

        org.activatePlan(plan, subscriptionId, customerId, periodEnd);
        organizationRepository.save(org);
        log.info("Plan activated: org={} plan={} sub={}", org.getId(), plan, subscriptionId);
    }

    private void handleRenewed(JsonNode data) {
        Organization org = resolveOrg(data);
        if (org == null) return;

        Instant newPeriodEnd = parseInstant(data.path("current_period_end").asText(null));
        org.renewPlan(newPeriodEnd);
        organizationRepository.save(org);
        log.info("Plan renewed: org={} newPeriodEnd={}", org.getId(), newPeriodEnd);
    }

    private void handleCancelled(JsonNode data) {
        // Org stays on current paid plan until the period ends (subscription.expired fires then)
        Organization org = resolveOrg(data);
        if (org == null) return;

        org.markSubscriptionCancelled();
        organizationRepository.save(org);
        log.info("Subscription cancelled: org={} — will stay on PRO until period ends", org.getId());
    }

    private void handleExpired(JsonNode data) {
        Organization org = resolveOrg(data);
        if (org == null) return;

        org.expirePlan();
        organizationRepository.save(org);
        log.info("Subscription expired: org={} — downgraded to STARTER", org.getId());
    }

    private void handleOnHold(JsonNode data) {
        Organization org = resolveOrg(data);
        if (org == null) return;

        org.holdPlan();
        organizationRepository.save(org);
        log.warn("Subscription on_hold (payment failure): org={}", org.getId());
    }

    // ------------------------------------------------------------------
    // Resolution helpers
    // ------------------------------------------------------------------

    /**
     * Resolves the Organization from the webhook payload.
     *
     * Primary: metadata.organizationId  (set by us at checkout creation time)
     * Fallback: subscription_id lookup  (handles edge cases / manual events)
     */
    private Organization resolveOrg(JsonNode data) {
        // Primary: metadata carries our internal organizationId
        String orgIdStr = data.path("metadata").path("organizationId").asText(null);
        if (orgIdStr != null && !orgIdStr.isBlank()) {
            try {
                UUID orgId = UUID.fromString(orgIdStr);
                Optional<Organization> org = organizationRepository.findById(orgId);
                if (org.isPresent()) return org.get();
                log.warn("Dodo webhook: org not found by metadata.organizationId={}", orgIdStr);
            } catch (IllegalArgumentException ex) {
                log.warn("Dodo webhook: invalid organizationId in metadata: {}", orgIdStr);
            }
        }

        // Fallback: look up by subscription ID
        String subscriptionId = data.path("subscription_id").asText(null);
        if (subscriptionId != null && !subscriptionId.isBlank()) {
            Optional<Organization> org = organizationRepository.findByDodoSubscriptionId(subscriptionId);
            if (org.isPresent()) return org.get();
            log.warn("Dodo webhook: org not found by subscription_id={}", subscriptionId);
        }

        log.warn("Dodo webhook: could not resolve organization — payload may be missing metadata");
        return null;
    }

    /**
     * Maps a Dodo product ID to the corresponding OrgPlan.
     * Recognises both the INR and USD PRO products.
     * Unknown product IDs default to PRO so the user is never left on STARTER.
     */
    private OrgPlan resolvePlan(String productId) {
        if (productId != null && (productId.equals(proProductIdInr) || productId.equals(proProductIdUsd))) {
            return OrgPlan.PRO;
        }
        log.warn("Dodo webhook: unrecognised product_id={} — defaulting to PRO", productId);
        return OrgPlan.PRO;
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Instant.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Signature verification (Standard Webhooks — identical to Resend)
    // ------------------------------------------------------------------

    /**
     * Verifies the Dodo Standard Webhooks HMAC-SHA256 signature.
     * Skips verification when DODO_WEBHOOK_SECRET is not configured (dev/local only).
     */
    private boolean verifySignature(
            String webhookId,
            String webhookTimestamp,
            String webhookSignature,
            String payload
    ) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return true; // unconfigured — skip (dev only)
        }
        if (webhookId == null || webhookTimestamp == null || webhookSignature == null) {
            return false;
        }
        try {
            // Secret may be prefixed with "whsec_" (Standard Webhooks convention)
            String secretBase64 = webhookSecret.startsWith("whsec_")
                    ? webhookSecret.substring("whsec_".length())
                    : webhookSecret;
            byte[] secretBytes = Base64.getDecoder().decode(secretBase64);

            String signedContent = webhookId + "." + webhookTimestamp + "." + payload;
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretBytes, HMAC_SHA256));
            String computed = Base64.getEncoder().encodeToString(
                    mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8)));

            // Header may contain multiple signatures: "v1,<sig1> v1,<sig2>"
            for (String part : webhookSignature.split(" ")) {
                if (part.startsWith("v1,") && part.substring(3).equals(computed)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            log.warn("Dodo webhook signature check threw exception: {}", ex.getMessage());
            return false;
        }
    }
}
