package com.flowcollect.infrastructure.whatsapp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.flowcollect.application.reminder.NotificationSender;
import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.invoice.followup.FollowUpChannel;
import com.flowcollect.exception.http.InternalException;
import com.flowcollect.infrastructure.config.WhatsAppCloudProperties;

@Component
public class WhatsAppSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppSender.class);

    private final WhatsAppCloudProperties properties;
    private final HttpClient httpClient;

    public WhatsAppSender(WhatsAppCloudProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newHttpClient();
    }

    @Override
    public FollowUpChannel channel() {
        return FollowUpChannel.WHATSAPP;
    }

    @Override
    public void send(Customer customer, String subject, String body) {
        ensureEnabled();

        String recipient = requireConfigured(customer.getPhone(), "customer phone");
        String whatsappBody = subject != null && !subject.isBlank()
                ? subject + System.lineSeparator() + System.lineSeparator() + (body == null ? "" : body)
                : (body == null ? "" : body);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildMessagesUrl()))
                    .header("Authorization", "Bearer " + requireConfigured(properties.getAccessToken(), "whatsapp.cloud.access-token"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildPayload(recipient, whatsappBody)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new InternalException("WhatsApp Cloud API returned status " + response.statusCode() + ": " + response.body());
            }

            log.info("Sent WhatsApp reminder to {}", recipient);
        } catch (Exception ex) {
            throw new InternalException("Failed to send WhatsApp reminder: " + ex.getMessage());
        }
    }

    private void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw new InternalException("WhatsApp Cloud delivery is disabled");
        }
    }

    private String requireConfigured(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new InternalException("Missing required WhatsApp Cloud configuration: " + propertyName);
        }
        return value.trim();
    }

    private String buildMessagesUrl() {
        return "https://graph.facebook.com/"
                + requireConfigured(properties.getApiVersion(), "whatsapp.cloud.api-version")
                + "/"
                + requireConfigured(properties.getPhoneNumberId(), "whatsapp.cloud.phone-number-id")
                + "/messages";
    }

    private String buildPayload(String recipient, String body) {
        return "{"
                + "\"messaging_product\":\"whatsapp\","
                + "\"to\":\"" + escapeJson(normalizeRecipient(recipient)) + "\","
                + "\"type\":\"text\","
                + "\"text\":{"
                + "\"preview_url\":false,"
                + "\"body\":\"" + escapeJson(body) + "\""
                + "}"
                + "}";
    }

    private String normalizeRecipient(String recipient) {
        String normalized = recipient == null ? "" : recipient.trim();
        if (normalized.startsWith("whatsapp:")) {
            normalized = normalized.substring("whatsapp:".length());
        }
        return normalized;
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }
}
