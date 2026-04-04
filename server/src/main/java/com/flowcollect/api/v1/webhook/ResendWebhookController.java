package com.flowcollect.api.v1.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowcollect.domain.invoice.followup.FollowUp;
import com.flowcollect.infrastructure.persistence.invoice.FollowUpJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

/**
 * Receives Resend email event webhooks.
 *
 * Resend signs webhook payloads using the svix signing scheme:
 *   signature_input = "{svix-id}.{svix-timestamp}.{raw-body}"
 *   signature       = HMAC-SHA256(base64Decode(secret), signature_input)
 *   svix-signature header = "v1,<base64(signature)>"
 *
 * Enable open tracking + configure this URL in your Resend dashboard.
 * Set RESEND_WEBHOOK_SECRET to the signing secret from the Resend dashboard.
 *
 * This endpoint is excluded from JWT auth via JwtFilter.SKIP_PREFIXES (/api/v1/webhooks/).
 */
@RestController
public class ResendWebhookController {

    private static final Logger log = LoggerFactory.getLogger(ResendWebhookController.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final FollowUpJpaRepository followUpRepository;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;

    public ResendWebhookController(
            FollowUpJpaRepository followUpRepository,
            ObjectMapper objectMapper,
            @Value("${RESEND_WEBHOOK_SECRET:}") String webhookSecret
    ) {
        this.followUpRepository = followUpRepository;
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret;
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("RESEND_WEBHOOK_SECRET is not set — Resend webhook signature verification will be skipped");
        }
    }

    /**
     * POST /api/v1/webhooks/resend
     *
     * Handles Resend email events. Currently processes:
     *   - email.opened: marks the corresponding FollowUp's openedAt timestamp.
     *
     * Returns 200 for all valid (or unverifiable) payloads to prevent Resend retries
     * on non-critical events.
     */
    @PostMapping("/api/v1/webhooks/resend")
    @Transactional
    public ResponseEntity<Void> handleResendWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "svix-id",        required = false) String svixId,
            @RequestHeader(value = "svix-timestamp", required = false) String svixTimestamp,
            @RequestHeader(value = "svix-signature", required = false) String svixSignature
    ) {
        if (!verifySignature(svixId, svixTimestamp, svixSignature, payload)) {
            log.warn("Resend webhook signature verification failed — rejecting request");
            return ResponseEntity.status(401).build();
        }

        try {
            JsonNode root      = objectMapper.readTree(payload);
            String   eventType = root.path("type").asText("");

            if ("email.clicked".equals(eventType) || "email.opened".equals(eventType)) {
                String emailId = root.path("data").path("email_id").asText(null);
                if (emailId == null || emailId.isBlank()) {
                    log.warn("{} event missing data.email_id — ignoring", eventType);
                    return ResponseEntity.ok().build();
                }
                handleEmailOpened(emailId);
            } else {
                log.debug("Unhandled Resend event type: {}", eventType);
            }
        } catch (Exception ex) {
            log.error("Error processing Resend webhook payload", ex);
            // Still return 200 to prevent Resend from retrying non-retryable errors
        }

        return ResponseEntity.ok().build();
    }

    private void handleEmailOpened(String emailId) {
        Optional<FollowUp> opt = followUpRepository.findByResendEmailId(emailId);
        if (opt.isEmpty()) {
            log.debug("email.opened — no FollowUp found for resendEmailId={}", emailId);
            return;
        }
        FollowUp followUp = opt.get();
        if (followUp.getOpenedAt() != null) {
            log.debug("email.opened — FollowUp {} already marked opened, skipping", followUp.getId());
            return;
        }
        followUp.markOpened(Instant.now());
        followUpRepository.save(followUp);
        log.info("email.opened — marked FollowUp {} as opened (resendEmailId={})", followUp.getId(), emailId);
    }

    /**
     * Verifies the svix webhook signature.
     * Skips verification if RESEND_WEBHOOK_SECRET is not configured (dev/local use only).
     */
    private boolean verifySignature(String svixId, String svixTimestamp, String svixSignature, String payload) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            return true; // unconfigured — skip verification
        }
        if (svixId == null || svixTimestamp == null || svixSignature == null) {
            return false;
        }
        try {
            // Decode secret: Resend secrets are base64-encoded (may have "whsec_" prefix)
            String secretBase64 = webhookSecret.startsWith("whsec_")
                    ? webhookSecret.substring("whsec_".length())
                    : webhookSecret;
            byte[] secretBytes = Base64.getDecoder().decode(secretBase64);

            String signedContent = svixId + "." + svixTimestamp + "." + payload;
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secretBytes, HMAC_SHA256));
            String computed = Base64.getEncoder().encodeToString(
                    mac.doFinal(signedContent.getBytes(StandardCharsets.UTF_8)));

            // svix-signature header may contain multiple signatures: "v1,<sig1> v1,<sig2>"
            for (String part : svixSignature.split(" ")) {
                if (part.startsWith("v1,") && part.substring(3).equals(computed)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            log.warn("Resend webhook signature check failed with exception: {}", ex.getMessage());
            return false;
        }
    }
}
