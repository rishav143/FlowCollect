package com.flowcollect.infrastructure.whatsapp;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.flowcollect.application.reminder.NotificationSender;
import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.followup.FollowUpChannel;
import com.flowcollect.exception.http.InternalException;
import com.flowcollect.infrastructure.config.WhatsAppCloudProperties;
import com.flowcollect.infrastructure.pdf.InvoicePdfGenerator;

@Component
public class WhatsAppSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppSender.class);

    private final WhatsAppCloudProperties properties;
    private final InvoicePdfGenerator pdfGenerator;
    private final HttpClient httpClient;

    public WhatsAppSender(WhatsAppCloudProperties properties, InvoicePdfGenerator pdfGenerator) {
        this.properties = properties;
        this.pdfGenerator = pdfGenerator;
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
        String whatsappBody = buildMessageText(subject, body);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildMessagesUrl()))
                    .header("Authorization", "Bearer " + requireConfigured(properties.getAccessToken(), "whatsapp.cloud.access-token"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildTextPayload(recipient, whatsappBody)))
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

    /**
     * Sends a WhatsApp message with the invoice PDF as a document attachment.
     * The flow follows the WhatsApp Cloud API two-step pattern:
     *   1. Upload the PDF bytes to the /media endpoint → receive a media_id
     *   2. Send a document message referencing that media_id (with the text as caption)
     */
    @Override
    public void send(Customer customer, String subject, String body, boolean attachPdf, Invoice invoice) {
        if (attachPdf && invoice != null) {
            sendWithDocument(customer, subject, body, invoice);
        } else {
            send(customer, subject, body);
        }
    }

    // --- Private helpers ---

    private void sendWithDocument(Customer customer, String subject, String body, Invoice invoice) {
        ensureEnabled();
        String recipient = requireConfigured(customer.getPhone(), "customer phone");
        String accessToken = requireConfigured(properties.getAccessToken(), "whatsapp.cloud.access-token");

        try {
            byte[] pdfBytes = pdfGenerator.generate(invoice);
            String filename = pdfGenerator.buildFileName(invoice);

            String mediaId = uploadMedia(pdfBytes, filename, accessToken);

            // The document caption is what the recipient sees below the file — reuse the message text.
            String caption = buildMessageText(subject, body);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(buildMessagesUrl()))
                    .header("Authorization", "Bearer " + accessToken)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildDocumentPayload(recipient, mediaId, filename, caption)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new InternalException("WhatsApp Cloud API returned status " + response.statusCode() + ": " + response.body());
            }

            log.info("Sent WhatsApp reminder with PDF attachment to {}", recipient);
        } catch (Exception ex) {
            throw new InternalException("Failed to send WhatsApp reminder with PDF: " + ex.getMessage());
        }
    }

    /**
     * Uploads PDF bytes to the WhatsApp Cloud media endpoint.
     * Returns the opaque media_id to be used in a subsequent document message.
     */
    private String uploadMedia(byte[] pdfBytes, String filename, String accessToken) throws Exception {
        String boundary = "----FlowCollectBoundary" + System.currentTimeMillis();
        byte[] multipartBody = buildMultipartBody(boundary, pdfBytes, filename);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(buildMediaUrl()))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new InternalException("WhatsApp media upload failed with status " + response.statusCode() + ": " + response.body());
        }

        return extractJsonStringField(response.body(), "id");
    }

    /**
     * Constructs the multipart/form-data body required by the WhatsApp Cloud /media endpoint.
     * Fields: messaging_product=whatsapp, type=application/pdf, file=<bytes>.
     */
    private byte[] buildMultipartBody(String boundary, byte[] pdfBytes, String filename) {
        String CRLF = "\r\n";
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        appendPart(out, boundary, "messaging_product", "whatsapp", CRLF);
        appendPart(out, boundary, "type", "application/pdf", CRLF);

        // File part — includes a Content-Type header
        appendString(out, "--" + boundary + CRLF);
        appendString(out, "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"" + CRLF);
        appendString(out, "Content-Type: application/pdf" + CRLF + CRLF);
        out.writeBytes(pdfBytes);
        appendString(out, CRLF);

        appendString(out, "--" + boundary + "--" + CRLF);
        return out.toByteArray();
    }

    private void appendPart(ByteArrayOutputStream out, String boundary, String name, String value, String CRLF) {
        appendString(out, "--" + boundary + CRLF);
        appendString(out, "Content-Disposition: form-data; name=\"" + name + "\"" + CRLF + CRLF);
        appendString(out, value + CRLF);
    }

    private void appendString(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Extracts a top-level string field from a simple JSON object.
     * Avoids a Jackson dependency for a single-field parse.
     */
    private String extractJsonStringField(String json, String field) {
        String key = "\"" + field + "\"";
        int keyIndex = json.indexOf(key);
        if (keyIndex < 0) {
            throw new InternalException("Field '" + field + "' not found in WhatsApp API response: " + json);
        }
        int colonIndex = json.indexOf(':', keyIndex + key.length());
        int startQuote = json.indexOf('"', colonIndex + 1);
        int endQuote = json.indexOf('"', startQuote + 1);
        if (startQuote < 0 || endQuote < 0) {
            throw new InternalException("Could not parse field '" + field + "' from WhatsApp API response: " + json);
        }
        return json.substring(startQuote + 1, endQuote);
    }

    private String buildMessageText(String subject, String body) {
        String formattedBody = formatForWhatsApp(body == null ? "" : body);
        String formattedSubject = subject != null && !subject.isBlank()
                ? formatForWhatsApp(subject)
                : null;
        return formattedSubject != null
                ? "*" + formattedSubject + "*\n\n" + formattedBody
                : formattedBody;
    }

    /**
     * Converts template body (written in our simplified syntax) to WhatsApp native markdown.
     *
     * Template syntax → WhatsApp:
     *   **text**   → *text*   (bold)
     *   *text*     → _text_   (italic)
     *   - item     → • item   (bullet)
     *   \n         → \n       (line breaks work natively)
     */
    private String formatForWhatsApp(String text) {
        String[] lines = text.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                sb.append("• ").append(convertInline(trimmed.substring(2)));
            } else {
                sb.append(convertInline(line));
            }
            if (i < lines.length - 1) sb.append("\n");
        }
        return sb.toString();
    }

    /** **bold** → *bold*  and  *italic* → _italic_  for WhatsApp rendering. */
    private String convertInline(String text) {
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "*$1*");
        text = text.replaceAll("\\*(.+?)\\*", "_$1_");
        return text;
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

    private String buildMediaUrl() {
        return "https://graph.facebook.com/"
                + requireConfigured(properties.getApiVersion(), "whatsapp.cloud.api-version")
                + "/"
                + requireConfigured(properties.getPhoneNumberId(), "whatsapp.cloud.phone-number-id")
                + "/media";
    }

    private String buildTextPayload(String recipient, String body) {
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

    private String buildDocumentPayload(String recipient, String mediaId, String filename, String caption) {
        return "{"
                + "\"messaging_product\":\"whatsapp\","
                + "\"to\":\"" + escapeJson(normalizeRecipient(recipient)) + "\","
                + "\"type\":\"document\","
                + "\"document\":{"
                + "\"id\":\"" + escapeJson(mediaId) + "\","
                + "\"filename\":\"" + escapeJson(filename) + "\","
                + "\"caption\":\"" + escapeJson(caption) + "\""
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
