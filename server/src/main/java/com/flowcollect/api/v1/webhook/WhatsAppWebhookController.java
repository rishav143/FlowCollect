package com.flowcollect.api.v1.webhook;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowcollect.application.invoice.FollowUpService;
import com.flowcollect.domain.invoice.followup.DeliveryStatus;
import com.flowcollect.infrastructure.config.WhatsAppCloudProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Receives Meta WhatsApp Cloud API webhook events.
 *
 * Two endpoints:
 *   GET  /api/v1/webhooks/whatsapp — hub verification challenge (one-time setup)
 *   POST /api/v1/webhooks/whatsapp — incoming events (message status updates)
 *
 * Signature verification (POST only):
 *   X-Hub-Signature-256: sha256=<HMAC-SHA256(appSecret, rawBody)>
 *
 * Status events we care about (entry.changes[].value.statuses[]):
 *   delivered → DeliveryStatus.DELIVERED
 *   read      → DeliveryStatus.DELIVERED  (read implies delivered)
 *   failed    → DeliveryStatus.UNDELIVERED
 *
 * This endpoint is excluded from JWT auth via JwtFilter.SKIP_PREFIXES (/api/v1/webhooks/).
 */
@RestController
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private final FollowUpService followUpService;
    private final WhatsAppCloudProperties properties;
    private final ObjectMapper objectMapper;

    public WhatsAppWebhookController(
            FollowUpService followUpService,
            WhatsAppCloudProperties properties,
            ObjectMapper objectMapper
    ) {
        this.followUpService = followUpService;
        this.properties      = properties;
        this.objectMapper    = objectMapper;
        if (properties.getAppSecret() == null || properties.getAppSecret().isBlank()) {
            log.warn("WHATSAPP_CLOUD_APP_SECRET is not set — WhatsApp webhook signature verification will be skipped");
        }
        if (properties.getWebhookVerifyToken() == null || properties.getWebhookVerifyToken().isBlank()) {
            log.warn("WHATSAPP_CLOUD_WEBHOOK_VERIFY_TOKEN is not set — hub challenge verification will accept any token");
        }
    }

    /**
     * GET /api/v1/webhooks/whatsapp
     *
     * Meta calls this once when you register the webhook URL in the Meta App dashboard.
     * Responds with hub.challenge when hub.verify_token matches our configured token.
     */
    @GetMapping("/api/v1/webhooks/whatsapp")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(value = "hub.mode",          required = false) String mode,
            @RequestParam(value = "hub.verify_token",  required = false) String token,
            @RequestParam(value = "hub.challenge",     required = false) String challenge
    ) {
        String configuredToken = properties.getWebhookVerifyToken();
        boolean tokenValid = configuredToken == null || configuredToken.isBlank()
                || configuredToken.equals(token);

        if ("subscribe".equals(mode) && tokenValid) {
            log.info("WhatsApp webhook verified successfully");
            return ResponseEntity.ok(challenge);
        }
        log.warn("WhatsApp webhook verification failed — mode={} token_match={}", mode, tokenValid);
        return ResponseEntity.status(403).build();
    }

    /**
     * POST /api/v1/webhooks/whatsapp
     *
     * Handles Meta WhatsApp Cloud API event payloads. Processes message status updates
     * (delivered, read, failed) to update the corresponding FollowUp's delivery status.
     */
    @PostMapping("/api/v1/webhooks/whatsapp")
    @Transactional
    public ResponseEntity<Void> handleWhatsAppEvent(
            @RequestBody String payload,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String hubSignature
    ) {
        if (!verifySignature(hubSignature, payload)) {
            log.warn("WhatsApp webhook signature verification failed — rejecting request");
            return ResponseEntity.status(403).build();
        }

        try {
            JsonNode root = objectMapper.readTree(payload);

            // Walk: entry[] → changes[] → value → statuses[]
            for (JsonNode entry : root.path("entry")) {
                for (JsonNode change : entry.path("changes")) {
                    JsonNode value    = change.path("value");
                    JsonNode statuses = value.path("statuses");
                    if (!statuses.isArray()) continue;

                    for (JsonNode statusNode : statuses) {
                        String messageId = statusNode.path("id").asText(null);
                        String status    = statusNode.path("status").asText(null);

                        if (messageId == null || status == null) continue;

                        log.debug("WhatsApp status event — messageId={} status={}", messageId, status);

                        DeliveryStatus deliveryStatus = switch (status.toLowerCase()) {
                            case "delivered", "read" -> DeliveryStatus.DELIVERED;
                            case "failed"            -> DeliveryStatus.UNDELIVERED;
                            default                  -> null; // sent, etc. — ignore
                        };

                        if (deliveryStatus != null) {
                            followUpService.updateDeliveryStatus(messageId, deliveryStatus);
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Error processing WhatsApp webhook payload", ex);
            // Return 200 to prevent Meta from retrying non-retryable errors
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Verifies Meta's X-Hub-Signature-256 header.
     * Format: "sha256=<hex(HMAC-SHA256(appSecret, rawBody))>"
     * Skips verification when WHATSAPP_CLOUD_APP_SECRET is not configured.
     */
    private boolean verifySignature(String hubSignature, String payload) {
        String appSecret = properties.getAppSecret();
        if (appSecret == null || appSecret.isBlank()) {
            return true; // unconfigured — skip (dev/local only)
        }
        if (hubSignature == null || !hubSignature.startsWith("sha256=")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            String computed = "sha256=" + hex;
            return computed.equals(hubSignature);
        } catch (Exception ex) {
            log.warn("WhatsApp webhook signature check failed: {}", ex.getMessage());
            return false;
        }
    }
}
