package com.flowcollect.infrastructure.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.flowcollect.application.reminder.NotificationSender;
import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.followup.FollowUpChannel;
import com.flowcollect.exception.http.InternalException;
import com.flowcollect.infrastructure.config.NotificationEmailProperties;
import com.flowcollect.infrastructure.pdf.ConsolidatedSummaryPdfGenerator;
import com.flowcollect.infrastructure.pdf.InvoicePdfGenerator;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EmailSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final ResendEmailClient emailClient;
    private final NotificationEmailProperties properties;
    private final InvoicePdfGenerator pdfGenerator;
    private final ConsolidatedSummaryPdfGenerator summaryPdfGenerator;

    public EmailSender(
            ResendEmailClient emailClient,
            NotificationEmailProperties properties,
            InvoicePdfGenerator pdfGenerator,
            ConsolidatedSummaryPdfGenerator summaryPdfGenerator
    ) {
        this.emailClient = emailClient;
        this.properties = properties;
        this.pdfGenerator = pdfGenerator;
        this.summaryPdfGenerator = summaryPdfGenerator;
    }

    @Override
    public FollowUpChannel channel() {
        return FollowUpChannel.EMAIL;
    }

    @Override
    public String send(Customer customer, String subject, String body) {
        return send(customer, subject, body, false, null);
    }

    @Override
    public String send(Customer customer, String subject, String body, boolean attachPdf, Invoice invoice) {
        if (!emailClient.isConfigured()) {
            throw new InternalException("Email delivery is not configured. Set RESEND_API_KEY to enable it.");
        }

        String recipient     = requireConfigured(customer.getEmail(), "customer email");
        String fromAddress   = requireConfigured(properties.getFromAddress(), "notification.email.from-address");
        String fromFormatted = properties.getFromName() + " <" + fromAddress + ">";
        String emailSubject  = subject == null || subject.isBlank() ? properties.getFromName() + " reminder" : subject;
        String html          = toHtml(body == null ? "" : body);

        try {
            if (attachPdf && invoice != null) {
                byte[] pdfBytes = pdfGenerator.generate(invoice);
                String emailId = emailClient.send(fromFormatted, recipient, emailSubject, html,
                        pdfGenerator.buildFileName(invoice), pdfBytes);
                log.info("Sent EMAIL reminder with PDF to {} subject '{}'", recipient, emailSubject);
                return emailId;
            } else {
                String emailId = emailClient.send(fromFormatted, recipient, emailSubject, html);
                log.info("Sent EMAIL reminder to {} subject '{}'", recipient, emailSubject);
                return emailId;
            }
        } catch (Exception ex) {
            throw new InternalException("Failed to send email reminder: " + ex.getMessage());
        }
    }

    // Matches http(s) URLs — stops at whitespace or common HTML-unsafe chars
    private static final Pattern URL_PATTERN = Pattern.compile("https?://[^\\s\"<>]+");

    private String toHtml(String plainText) {
        String[] lines = plainText.split("\n", -1);
        StringBuilder bodyHtml = new StringBuilder();
        boolean inList = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                if (!inList) {
                    bodyHtml.append("<ul style=\"margin:8px 0 8px 0;padding-left:22px;\">");
                    inList = true;
                }
                bodyHtml.append("<li>").append(inlineFormat(trimmed.substring(2))).append("</li>");
            } else {
                if (inList) {
                    bodyHtml.append("</ul>");
                    inList = false;
                }
                if (trimmed.isEmpty()) {
                    bodyHtml.append("<br>");
                } else {
                    bodyHtml.append(renderTextLine(line));
                }
            }
        }
        if (inList) bodyHtml.append("</ul>");

        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f4f4f5;font-family:Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:40px 0;">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0"
                             style="background:#ffffff;border-radius:8px;padding:40px;
                                    box-shadow:0 1px 4px rgba(0,0,0,.08);">
                        <tr><td style="font-size:15px;line-height:1.7;color:#374151;">
                          %s
                        </td></tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(bodyHtml);
    }

    /**
     * Renders a non-empty, non-list line. Any URLs found anywhere in the line
     * are extracted and rendered as centered CTA buttons; surrounding text is
     * rendered as normal formatted text.
     */
    private String renderTextLine(String line) {
        Matcher m = URL_PATTERN.matcher(line.trim());
        if (!m.find()) {
            return inlineFormat(line) + "<br>";
        }

        StringBuilder sb = new StringBuilder();
        m.reset();
        int last = 0;
        while (m.find()) {
            // Text before the URL
            if (m.start() > last) {
                String before = inlineFormat(line.trim().substring(last, m.start())).stripTrailing();
                if (!before.isEmpty()) sb.append(before).append("<br>");
            }
            // Strip trailing punctuation that is not part of the URL
            String raw     = m.group();
            String url     = raw.replaceAll("[.,;:!?)]+$", "");
            String trailer = raw.substring(url.length());
            // CTA button
            sb.append("<div style=\"text-align:center;margin:20px 0;\">")
              .append("<a href=\"").append(url).append("\" ")
              .append("style=\"display:inline-block;padding:12px 28px;")
              .append("background:#29B6F6;color:#ffffff;text-decoration:none;")
              .append("border-radius:6px;font-weight:bold;font-size:15px;\">")
              .append("I&#39;ve Paid")
              .append("</a></div>");
            if (!trailer.isEmpty()) sb.append(trailer);
            last = m.end();
        }
        // Any text after the last URL
        if (last < line.trim().length()) {
            String after = inlineFormat(line.trim().substring(last)).stripLeading();
            if (!after.isEmpty()) sb.append(after).append("<br>");
        }
        return sb.toString();
    }

    private String inlineFormat(String text) {
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        text = text.replaceAll("\\*(.+?)\\*", "<em>$1</em>");
        return text;
    }

    @Override
    public String sendConsolidated(Customer customer, String subject, String body,
                                   List<Invoice> invoices) {
        if (!emailClient.isConfigured()) {
            throw new InternalException("Email delivery is not configured. Set RESEND_API_KEY to enable it.");
        }

        String recipient     = requireConfigured(customer.getEmail(), "customer email");
        String fromAddress   = requireConfigured(properties.getFromAddress(), "notification.email.from-address");
        String fromFormatted = properties.getFromName() + " <" + fromAddress + ">";
        String emailSubject  = subject == null || subject.isBlank() ? properties.getFromName() + " reminder" : subject;
        String html          = toHtml(body == null ? "" : body);

        try {
            // Always attach a consolidated statement PDF — one clean document per customer
            byte[] summaryPdf = summaryPdfGenerator.generate(invoices, customer);
            String fileName   = summaryPdfGenerator.buildFileName(customer);
            List<ResendEmailClient.Attachment> attachments =
                    List.of(new ResendEmailClient.Attachment(fileName, summaryPdf));

            String emailId = emailClient.send(fromFormatted, recipient, emailSubject, html, attachments);
            log.info("Sent consolidated EMAIL with statement PDF to {} subject '{}'", recipient, emailSubject);
            return emailId;
        } catch (Exception ex) {
            throw new InternalException("Failed to send consolidated email: " + ex.getMessage());
        }
    }

    private String requireConfigured(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new InternalException("Missing required email configuration: " + propertyName);
        }
        return value.trim();
    }
}
