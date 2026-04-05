package com.flowcollect.infrastructure.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends transactional emails via the Resend HTTP API (https://api.resend.com/emails).
 *
 * Uses HTTPS (port 443) instead of SMTP so it works on any cloud provider
 * regardless of SMTP port restrictions (e.g. Render blocks 465/587).
 *
 * Requires the RESEND_API_KEY environment variable. If not set, all send
 * calls are skipped with a warning log — the application still starts.
 */
@Component
public class ResendEmailClient {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailClient.class);
    private static final String API_URL = "https://api.resend.com/emails";

    private final RestTemplate restTemplate = new RestTemplate();
    private final String apiKey;

    public ResendEmailClient(@Value("${RESEND_API_KEY:}") String apiKey) {
        this.apiKey = apiKey;
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("RESEND_API_KEY is not set — email delivery will be skipped");
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Send a plain HTML email with no attachment. Returns the Resend email ID, or null if not configured. */
    public String send(String from, String to, String subject, String html) {
        return send(from, to, subject, html, null, null);
    }

    /** Send an HTML email with an optional binary attachment (e.g. a PDF). Returns the Resend email ID, or null if not configured. */
    public String send(String from, String to, String subject, String html,
                       String attachmentFilename, byte[] attachmentBytes) {
        List<Attachment> attachments = (attachmentFilename != null && attachmentBytes != null)
                ? List.of(new Attachment(attachmentFilename, attachmentBytes))
                : List.of();
        return send(from, to, subject, html, attachments);
    }

    /**
     * Send an HTML email with multiple binary attachments (e.g. one PDF per invoice).
     * Returns the Resend email ID, or null if not configured.
     */
    @SuppressWarnings("unchecked")
    public String send(String from, String to, String subject, String html,
                       List<Attachment> attachments) {
        if (!isConfigured()) {
            log.warn("RESEND_API_KEY not configured — skipping email to {}", to);
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", from);
        body.put("to", List.of(to));
        body.put("subject", subject);
        body.put("html", html);

        if (attachments != null && !attachments.isEmpty()) {
            List<Map<String, String>> attachmentList = attachments.stream()
                    .map(a -> {
                        Map<String, String> m = new LinkedHashMap<>();
                        m.put("filename", a.filename());
                        m.put("content", Base64.getEncoder().encodeToString(a.bytes()));
                        return m;
                    })
                    .toList();
            body.put("attachments", attachmentList);
        }

        Map<String, Object> response = restTemplate.postForObject(
                API_URL, new HttpEntity<>(body, headers), Map.class);
        String emailId = response != null ? (String) response.get("id") : null;
        log.debug("Email sent via Resend HTTP API to {} — id={}", to, emailId);
        return emailId;
    }

    /** Value type for a named binary attachment. */
    public record Attachment(String filename, byte[] bytes) {}
}
