package com.flowcollect.infrastructure.email;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.ByteArrayResource;

@Component
public class EmailSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(EmailSender.class);

    private final JavaMailSender mailSender;
    private final NotificationEmailProperties properties;
    private final InvoicePdfGenerator pdfGenerator;

    public EmailSender(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            NotificationEmailProperties properties,
            InvoicePdfGenerator pdfGenerator
    ) {
        this.mailSender = mailSenderProvider.getIfAvailable();
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
        if (mailSender == null) {
            throw new InternalException("Email delivery is not configured. Set spring.mail.* properties to enable it.");
        }

        String recipient = requireConfigured(customer.getEmail(), "customer email");
        String fromAddress = requireConfigured(properties.getFromAddress(), "notification.email.from-address");
        String emailSubject = subject == null || subject.isBlank() ? properties.getFromName()+" reminder" : subject;
        String emailBody = body == null ? "" : body;

        try {
            MimeMessage message = mailSender.createMimeMessage();
            String html = toHtml(emailBody);

            if (attachPdf && invoice != null) {
                // multipart/mixed so we can attach the PDF alongside the HTML body
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setTo(recipient);
                helper.setFrom(fromAddress);
                helper.setSubject(emailSubject);
                helper.setText(toPlainText(emailBody), html);
                byte[] pdfBytes = pdfGenerator.generate(invoice);
                helper.addAttachment(pdfGenerator.buildFileName(invoice), new ByteArrayResource(pdfBytes));
                log.info("Sent EMAIL reminder with PDF to {} subject '{}'", recipient, emailSubject);
            } else {
                // text/html only — no plain-text alternative so the client always renders HTML
                MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
                helper.setTo(recipient);
                helper.setFrom(fromAddress);
                helper.setSubject(emailSubject);
                helper.setText(html, true);
                log.info("Sent EMAIL reminder to {} subject '{}'", recipient, emailSubject);
            }

            mailSender.send(message);
        } catch (Exception ex) {
            throw new InternalException("Failed to send email reminder: " + ex.getMessage());
        }
    }

    /**
     * Converts a plain-text template body (with simple Markdown-like syntax)
     * into an HTML email body wrapped in a basic card layout.
     *
     * Supported syntax:
     *   **text**       → bold
     *   *text*         → italic
     *   - item         → bullet list item
     *   blank line     → paragraph break
     *   regular line   → line break
     */
    private String toHtml(String plainText) {
        String[] lines = plainText.split("\n", -1);
        StringBuilder body = new StringBuilder();
        boolean inList = false;

        for (String line : lines) {
            String trimmed = line.trim();

            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                if (!inList) {
                    body.append("<ul style=\"margin:8px 0 8px 0;padding-left:22px;\">");
                    inList = true;
                }
                body.append("<li>").append(inlineFormat(trimmed.substring(2))).append("</li>");
            } else {
                if (inList) {
                    body.append("</ul>");
                    inList = false;
                }
                if (trimmed.isEmpty()) {
                    body.append("<br>");
                } else {
                    body.append(inlineFormat(line)).append("<br>");
                }
            }
        }
        if (inList) body.append("</ul>");

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
                """.formatted(body);
    }

    /** Strips **bold** and *italic* markers for the plain-text fallback. */
    private String toPlainText(String text) {
        text = text.replaceAll("\\*\\*(.+?)\\*\\*", "$1");
        text = text.replaceAll("\\*(.+?)\\*", "$1");
        return text;
    }

    /** Converts **bold** and *italic* markers to HTML inline elements. */
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
