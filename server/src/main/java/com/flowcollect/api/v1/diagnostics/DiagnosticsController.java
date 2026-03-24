package com.flowcollect.api.v1.diagnostics;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.flowcollect.infrastructure.config.NotificationEmailProperties;
import com.flowcollect.infrastructure.config.TwilioProperties;
import com.flowcollect.infrastructure.email.ResendEmailClient;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight diagnostics endpoint for verifying infrastructure integrations
 * during local development and staging.
 *
 * This controller is excluded from JWT auth via JwtFilter#SKIP_PREFIXES.
 * Remove or guard it before going to production.
 */
@RestController
@RequestMapping("/api/v1/diagnostics")
public class DiagnosticsController {

    private final ResendEmailClient emailClient;
    private final NotificationEmailProperties emailProperties;
    private final TwilioProperties twilioProperties;

    public DiagnosticsController(
            ResendEmailClient emailClient,
            NotificationEmailProperties emailProperties,
            TwilioProperties twilioProperties
    ) {
        this.emailClient = emailClient;
        this.emailProperties = emailProperties;
        this.twilioProperties = twilioProperties;
    }

    /**
     * Sends a test email via the Resend HTTP API.
     * Usage: POST /api/v1/diagnostics/test-email?to=you@example.com
     */
    @PostMapping("/test-email")
    public ResponseEntity<Map<String, String>> testEmail(@RequestParam String to) {
        Map<String, String> result = new LinkedHashMap<>();

        if (!emailClient.isConfigured()) {
            result.put("status", "error");
            result.put("message", "RESEND_API_KEY is not set");
            return ResponseEntity.status(503).body(result);
        }

        String from = emailProperties.getFromAddress();
        if (from == null || from.isBlank()) {
            result.put("status", "error");
            result.put("message", "notification.email.from-address is not set");
            return ResponseEntity.status(503).body(result);
        }

        try {
            emailClient.send(
                emailProperties.getFromName() + " <" + from + ">",
                to,
                "FlowCollect – email delivery test",
                "<p>This is a test email sent by FlowCollect's diagnostics endpoint.</p>" +
                "<p>If you received this, Resend HTTP API is configured correctly.</p>"
            );

            result.put("status", "sent");
            result.put("to", to);
            result.put("from", from);
            result.put("message", "Email dispatched successfully via Resend HTTP API");
            return ResponseEntity.ok(result);

        } catch (Exception ex) {
            result.put("status", "error");
            result.put("to", to);
            result.put("from", from);
            result.put("message", ex.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Sends a probe SMS via Twilio.
     * Usage: POST /api/v1/diagnostics/test-sms?to=+91XXXXXXXXXX
     */
    @PostMapping("/test-sms")
    public ResponseEntity<Map<String, String>> testSms(@RequestParam String to) {
        Map<String, String> result = new LinkedHashMap<>();

        if (!twilioProperties.isEnabled()) {
            result.put("status", "error");
            result.put("message", "Twilio is disabled – set TWILIO_ENABLED=true");
            return ResponseEntity.status(503).body(result);
        }

        String accountSid = twilioProperties.getAccountSid();
        String authToken  = twilioProperties.getAuthToken();
        String from       = twilioProperties.getSmsFrom();

        if (accountSid == null || accountSid.isBlank() ||
            authToken  == null || authToken.isBlank()  ||
            from       == null || from.isBlank()) {
            result.put("status", "error");
            result.put("message", "Twilio credentials incomplete – check TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_SMS_FROM");
            return ResponseEntity.status(503).body(result);
        }

        try {
            Twilio.init(accountSid, authToken);
            Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(from),
                    "FlowCollect SMS test – if you received this, Twilio is configured correctly."
            ).create();

            result.put("status", "sent");
            result.put("to", to);
            result.put("from", from);
            result.put("message", "SMS dispatched successfully via Twilio");
            return ResponseEntity.ok(result);

        } catch (Exception ex) {
            result.put("status", "error");
            result.put("to", to);
            result.put("from", from);
            result.put("message", ex.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
}
