package com.flowcollect.api.v1.diagnostics;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.flowcollect.infrastructure.config.NotificationEmailProperties;
import com.flowcollect.infrastructure.config.TwilioProperties;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight diagnostics endpoint for verifying infrastructure integrations
 * (e.g. Resend SMTP) during local development and staging.
 *
 * This controller is excluded from JWT auth via JwtFilter#SKIP_PREFIXES.
 * Remove or guard it before going to production.
 */
@RestController
@RequestMapping("/api/v1/diagnostics")
public class DiagnosticsController {

    private final JavaMailSender mailSender;
    private final NotificationEmailProperties emailProperties;
    private final TwilioProperties twilioProperties;

    public DiagnosticsController(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            NotificationEmailProperties emailProperties,
            TwilioProperties twilioProperties
    ) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.emailProperties = emailProperties;
        this.twilioProperties = twilioProperties;
    }

    /**
     * Sends a plain-text probe email via the configured Resend SMTP credentials.
     *
     * Usage:
     *   POST /api/v1/diagnostics/test-email?to=you@example.com
     *
     * Returns a JSON object with:
     *   { "status": "sent"|"error", "to": "...", "from": "...", "message": "..." }
     */
    @PostMapping("/test-email")
    public ResponseEntity<Map<String, String>> testEmail(
            @RequestParam String to
    ) {
        Map<String, String> result = new LinkedHashMap<>();

        if (mailSender == null) {
            result.put("status", "error");
            result.put("message", "JavaMailSender is not configured – set spring.mail.* properties");
            return ResponseEntity.status(503).body(result);
        }

        String from = emailProperties.getFromAddress();
        if (from == null || from.isBlank()) {
            result.put("status", "error");
            result.put("message", "notification.email.from-address is not set");
            return ResponseEntity.status(503).body(result);
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setFrom(from);
            msg.setSubject("FlowCollect – email delivery test");
            msg.setText(
                "This is a test email sent by FlowCollect's diagnostics endpoint.\n\n" +
                "If you received this, Resend SMTP is configured correctly.\n\n" +
                "From: " + from
            );
            mailSender.send(msg);

            result.put("status", "sent");
            result.put("to", to);
            result.put("from", from);
            result.put("message", "Email dispatched successfully via Resend SMTP");
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
     * Sends a probe SMS via the configured Twilio credentials.
     *
     * Usage:
     *   POST /api/v1/diagnostics/test-sms?to=+91XXXXXXXXXX
     */
    @PostMapping("/test-sms")
    public ResponseEntity<Map<String, String>> testSms(
            @RequestParam String to
    ) {
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
            result.put("message", "Twilio credentials are incomplete – check TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_SMS_FROM");
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
