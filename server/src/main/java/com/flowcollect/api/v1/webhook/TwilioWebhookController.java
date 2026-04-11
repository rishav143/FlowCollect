package com.flowcollect.api.v1.webhook;

import com.flowcollect.application.invoice.FollowUpService;
import com.flowcollect.domain.invoice.followup.DeliveryStatus;
import com.flowcollect.infrastructure.config.TwilioProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;

/**
 * Receives Twilio SMS delivery status callbacks.
 *
 * Twilio posts form-encoded status updates to the statusCallback URL registered
 * when the message was created. Key fields: MessageSid, MessageStatus.
 *
 * Signature verification:
 *   1. Sort all POST params alphabetically by key, concatenate key+value pairs.
 *   2. Prepend the full callback URL.
 *   3. HMAC-SHA1 with the Twilio auth token → Base64 → compare with X-Twilio-Signature.
 *
 * This endpoint is excluded from JWT auth via JwtFilter.SKIP_PREFIXES (/api/v1/webhooks/).
 */
@RestController
public class TwilioWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TwilioWebhookController.class);
    private static final String HMAC_SHA1 = "HmacSHA1";

    private final FollowUpService followUpService;
    private final TwilioProperties twilioProperties;
    private final String appBaseUrl;

    public TwilioWebhookController(
            FollowUpService followUpService,
            TwilioProperties twilioProperties,
            @Value("${app.base-url}") String appBaseUrl
    ) {
        this.followUpService  = followUpService;
        this.twilioProperties = twilioProperties;
        this.appBaseUrl       = appBaseUrl;
        if (twilioProperties.getAuthToken() == null || twilioProperties.getAuthToken().isBlank()) {
            log.warn("TWILIO_AUTH_TOKEN is not set — Twilio webhook signature verification will be skipped");
        }
    }

    /**
     * POST /api/v1/webhooks/twilio/status
     *
     * Handles Twilio SMS message status callbacks. Relevant statuses:
     *   delivered   → DeliveryStatus.DELIVERED
     *   failed      → DeliveryStatus.UNDELIVERED
     *   undelivered → DeliveryStatus.UNDELIVERED
     *
     * Other statuses (queued, sent, sending) are acknowledged but ignored.
     */
    @PostMapping(value = "/api/v1/webhooks/twilio/status", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @Transactional
    public ResponseEntity<Void> handleTwilioStatus(
            @RequestParam Map<String, String> params,
            @RequestHeader(value = "X-Twilio-Signature", required = false) String twilioSignature
    ) {
        String callbackUrl = appBaseUrl + "/api/v1/webhooks/twilio/status";

        if (!verifySignature(twilioSignature, callbackUrl, params)) {
            log.warn("Twilio webhook signature verification failed — rejecting request");
            return ResponseEntity.status(403).build();
        }

        String messageSid    = params.get("MessageSid");
        String messageStatus = params.get("MessageStatus");

        if (messageSid == null || messageStatus == null) {
            log.warn("Twilio webhook missing MessageSid or MessageStatus");
            return ResponseEntity.badRequest().build();
        }

        log.debug("Twilio status callback — SID={} status={}", messageSid, messageStatus);

        DeliveryStatus deliveryStatus = switch (messageStatus.toLowerCase()) {
            case "delivered"   -> DeliveryStatus.DELIVERED;
            case "failed",
                 "undelivered" -> DeliveryStatus.UNDELIVERED;
            default            -> null; // queued, sent, sending — ignore
        };

        if (deliveryStatus != null) {
            followUpService.updateDeliveryStatus(messageSid, deliveryStatus);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Verifies Twilio's X-Twilio-Signature using HMAC-SHA1.
     * Skips verification when TWILIO_AUTH_TOKEN is not configured (dev/local only).
     *
     * Algorithm:
     *   signingInput = callbackUrl + sortedParams(key1 + value1 + key2 + value2 ...)
     *   expected     = Base64(HMAC-SHA1(authToken, signingInput))
     */
    private boolean verifySignature(String twilioSignature, String callbackUrl, Map<String, String> params) {
        String authToken = twilioProperties.getAuthToken();
        if (authToken == null || authToken.isBlank()) {
            return true; // unconfigured — skip (dev/local only)
        }
        if (twilioSignature == null || twilioSignature.isBlank()) {
            return false;
        }
        try {
            // Sort params alphabetically and concatenate key+value
            StringBuilder sb = new StringBuilder(callbackUrl);
            new TreeMap<>(params).forEach((k, v) -> sb.append(k).append(v == null ? "" : v));

            Mac mac = Mac.getInstance(HMAC_SHA1);
            mac.init(new SecretKeySpec(authToken.getBytes(StandardCharsets.UTF_8), HMAC_SHA1));
            String computed = Base64.getEncoder().encodeToString(
                    mac.doFinal(sb.toString().getBytes(StandardCharsets.UTF_8)));

            return computed.equals(twilioSignature);
        } catch (Exception ex) {
            log.warn("Twilio webhook signature check failed: {}", ex.getMessage());
            return false;
        }
    }
}
