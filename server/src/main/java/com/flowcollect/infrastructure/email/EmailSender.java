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
import com.flowcollect.infrastructure.pdf.InvoicePdfGenerator;

@Component
public class EmailSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final ResendEmailClient emailClient;
    private final NotificationEmailProperties properties;
    private final InvoicePdfGenerator pdfGenerator;

    public EmailSender(
            ResendEmailClient emailClient,
            NotificationEmailProperties properties,
            InvoicePdfGenerator pdfGenerator
    ) {
        this.emailClient = emailClient;
        this.properties = properties;
        this.pdfGenerator = pdfGenerator;
    }

    @Override
    public FollowUpChannel channel() {
        return FollowUpChannel.EMAIL;
    }

    @Override
    public void send(Customer customer, String subject, String body) {
        send(customer, subject, body, false, null);
    }

    @Override
    public void send(Customer customer, String subject, String body, boolean attachPdf, Invoice invoice) {
        if (!emailClient.isConfigured()) {
            throw new InternalException("Email delivery is not configured. Set RESEND_API_KEY to enable it.");
        }

        String recipient  = requireConfigured(customer.getEmail(), "customer email");
        String fromAddress = requireConfigured(properties.getFromAddress(), "notification.email.from-address");
        String fromFormatted = properties.getFromName() + " <" + fromAddress + ">";
        String emailSubject = subject == null || subject.isBlank() ? properties.getFromName() + " reminder" : subject;
        String emailBody    = body == null ? "" : body;
        String html         = toHtml(emailBody);

        try {
            if (attachPdf && invoice != null) {
                byte[] pdfBytes = pdfGenerator.generate(invoice);
                emailClient.send(fromFormatted, recipient, emailSubject, html,
                        pdfGenerator.buildFileName(invoice), pdfBytes);
                log.info("Sent EMAIL reminder with PDF to {} subject '{}'", recipient, emailSubject);
            } else {
                emailClient.send(fromFormatted, recipient, emailSubject, html);
                log.info("Sent EMAIL reminder to {} subject '{}'", recipient, emailSubject);
            }
        } catch (Exception ex) {
            throw new InternalException("Failed to send email reminder: " + ex.getMessage());
        }
    }

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
                    bodyHtml.append(inlineFormat(line)).append("<br>");
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

    private String inlineFormat(String text) {
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "<strong>$1</strong>");
        text = text.replaceAll("\\*(.+?)\\*", "<em>$1</em>");
        return text;
    }

    private String requireConfigured(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new InternalException("Missing required email configuration: " + propertyName);
        }
        return value.trim();
    }
}
