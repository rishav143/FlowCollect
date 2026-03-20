package com.flowcollect.application.confirmation;

import java.math.BigDecimal;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.flowcollect.domain.confirmation.PaymentConfirmation;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.infrastructure.config.NotificationEmailProperties;

import jakarta.mail.internet.MimeMessage;

/**
 * Sends internal (business-facing) and customer-facing email notifications
 * about payment confirmation events.
 *
 * <p>All methods are fire-and-forget: delivery failures are logged as warnings and
 * never propagate as exceptions, so a broken SMTP connection cannot block the
 * confirmation workflow.
 */
@Service
public class BusinessNotificationService {

    private static final Logger log = LoggerFactory.getLogger(BusinessNotificationService.class);

    private final Optional<JavaMailSender> mailSender;
    private final NotificationEmailProperties emailProperties;

    public BusinessNotificationService(Optional<JavaMailSender> mailSender, NotificationEmailProperties emailProperties) {
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
        if (mailSender.isEmpty()) {
            log.warn("JavaMailSender is not configured — business notification emails will be skipped");
        }
    }

    /**
     * Notifies the organization that a customer has submitted a payment claim.
     */
    public void notifyPaymentSubmitted(Invoice invoice, PaymentConfirmation confirmation) {
        String subject = "Payment Claim Received — Invoice #" + invoice.getInvoiceNumber();
        String html = buildSubmittedHtml(invoice, confirmation);
        sendEmail(emailProperties.getFromAddress(), null, invoice.getOrganization().getEmail(), subject, html);
    }

    /**
     * Notifies the customer that their payment confirmation was approved.
     */
    public void notifyCustomerApproved(Invoice invoice, PaymentConfirmation confirmation) {
        String customerEmail = invoice.getCustomer() != null ? invoice.getCustomer().getEmail() : null;
        if (customerEmail == null || customerEmail.isBlank()) {
            log.info("Skipping customer approval notification — no email on customer for invoice {}", invoice.getId());
            return;
        }
        String subject = "Payment Confirmed — Invoice #" + invoice.getInvoiceNumber();
        String html = buildApprovedHtml(invoice, confirmation);
        sendEmail(emailProperties.getFromAddress(), invoice.getOrganization().getEmail(), customerEmail, subject, html);
    }

    /**
     * Notifies the customer that their payment confirmation was rejected.
     */
    public void notifyCustomerRejected(Invoice invoice, PaymentConfirmation confirmation) {
        String customerEmail = invoice.getCustomer() != null ? invoice.getCustomer().getEmail() : null;
        if (customerEmail == null || customerEmail.isBlank()) {
            log.info("Skipping customer rejection notification — no email on customer for invoice {}", invoice.getId());
            return;
        }
        String subject = "Payment Not Verified — Invoice #" + invoice.getInvoiceNumber();
        String html = buildRejectedHtml(invoice, confirmation);
        sendEmail(emailProperties.getFromAddress(), invoice.getOrganization().getEmail(), customerEmail, subject, html);
    }

    // -----------------------------------------------------------------------
    // HTML builders
    // -----------------------------------------------------------------------

    private String buildSubmittedHtml(Invoice invoice, PaymentConfirmation confirmation) {
        String currency = invoice.getOrganization().getCurrency().getCurrencyCode();
        String orgName = invoice.getOrganization().getName();
        String appName = emailProperties.getFromName();

        // Balance AFTER this claim would be applied (what will remain if approved)
        BigDecimal balanceAfterApproval = invoice.getRemainingAmount().subtract(confirmation.getAmountClaimed());
        if (balanceAfterApproval.compareTo(BigDecimal.ZERO) < 0) {
            balanceAfterApproval = BigDecimal.ZERO;
        }
        String balanceColor = balanceAfterApproval.compareTo(BigDecimal.ZERO) == 0 ? "#16a34a" : "#374151";

        String customerNoteRow = confirmation.getCustomerNote() != null
                ? "<tr>"
                  + "<td style=\"padding:8px 0;color:#6b7280;font-size:13px;vertical-align:top;\"><strong>Customer note</strong></td>"
                  + "<td style=\"padding:8px 0;color:#374151;font-size:13px;\">" + escapeHtml(confirmation.getCustomerNote()) + "</td>"
                  + "</tr>"
                : "";

        return "<!DOCTYPE html>"
                + "<html><body style=\"margin:0;padding:0;background:#f4f4f5;font-family:Arial,sans-serif;\">"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">"
                + "<tr><td align=\"center\" style=\"padding:40px 20px;\">"
                + "<table cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;max-width:560px;background:#ffffff;"
                + "border-radius:8px;padding:36px;box-shadow:0 1px 4px rgba(0,0,0,.08);\">"
                + "<tr><td style=\"font-size:15px;line-height:1.7;color:#374151;\">"
                + "<p style=\"margin:0 0 6px;font-size:13px;color:#6b7280;\">Invoice #" + escapeHtml(invoice.getInvoiceNumber()) + "</p>"
                + "<p style=\"margin:0 0 20px;font-size:18px;font-weight:bold;color:#111827;\">Payment Claim Received</p>"
                + "<p style=\"margin:0 0 24px;\">Hi <strong>" + escapeHtml(orgName) + " team</strong>, a customer has submitted a payment claim. Please log in to review and approve or reject it.</p>"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#f9fafb;border-radius:6px;padding:16px 20px;margin-bottom:24px;\">"
                + "<tr>"
                + "<td style=\"padding:8px 0;color:#6b7280;font-size:13px;\"><strong>Claimed amount</strong></td>"
                + "<td style=\"padding:8px 0;color:#374151;font-size:13px;\">" + escapeHtml(confirmation.getAmountClaimed().toPlainString()) + " " + currency + "</td>"
                + "</tr>"
                + "<tr>"
                + "<td style=\"padding:8px 0;color:#6b7280;font-size:13px;\"><strong>Balance after approval</strong></td>"
                + "<td style=\"padding:8px 0;font-size:13px;font-weight:bold;color:" + balanceColor + ";\">" + escapeHtml(balanceAfterApproval.toPlainString()) + " " + currency + "</td>"
                + "</tr>"
                + customerNoteRow
                + "</table>"
                + "<hr style=\"border:none;border-top:1px solid #e5e7eb;margin:24px 0;\">"
                + "<p style=\"color:#9ca3af;font-size:12px;margin:0;\">— " + escapeHtml(appName) + "</p>"
                + "</td></tr></table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    private String buildApprovedHtml(Invoice invoice, PaymentConfirmation confirmation) {
        String currency = invoice.getOrganization().getCurrency().getCurrencyCode();
        String customerName = invoice.getCustomer().getName();
        String orgName = invoice.getOrganization().getName();
        String appName = emailProperties.getFromName();

        String businessNoteBlock = confirmation.getBusinessNote() != null
                ? "<p style=\"background:#f0fdf4;border-left:4px solid #16a34a;padding:12px 16px;"
                  + "border-radius:4px;color:#374151;margin:20px 0;font-size:14px;\">"
                  + "<strong>Note from " + escapeHtml(orgName) + ":</strong><br>"
                  + escapeHtml(confirmation.getBusinessNote()) + "</p>"
                : "";

        return "<!DOCTYPE html>"
                + "<html><body style=\"margin:0;padding:0;background:#f4f4f5;font-family:Arial,sans-serif;\">"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">"
                + "<tr><td align=\"center\" style=\"padding:40px 20px;\">"
                + "<table cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;max-width:560px;background:#ffffff;"
                + "border-radius:8px;padding:36px;box-shadow:0 1px 4px rgba(0,0,0,.08);\">"
                + "<tr><td style=\"font-size:15px;line-height:1.7;color:#374151;\">"
                + "<p style=\"margin:0 0 6px;font-size:13px;color:#6b7280;\">Invoice #" + escapeHtml(invoice.getInvoiceNumber()) + "</p>"
                + "<p style=\"margin:0 0 20px;font-size:18px;font-weight:bold;color:#16a34a;\">Payment Confirmed ✓</p>"
                + "<p style=\"margin:0 0 16px;\">Hi <strong>" + escapeHtml(customerName) + "</strong>,</p>"
                + "<p style=\"margin:0 0 16px;\">Great news! Your payment of <strong>" + escapeHtml(confirmation.getAmountClaimed().toPlainString()) + " " + currency + "</strong>"
                + " for <strong>Invoice #" + escapeHtml(invoice.getInvoiceNumber()) + "</strong> has been confirmed by <strong>" + escapeHtml(orgName) + "</strong>. Thank you!</p>"
                + businessNoteBlock
                + "<hr style=\"border:none;border-top:1px solid #e5e7eb;margin:24px 0;\">"
                + "<p style=\"color:#9ca3af;font-size:12px;margin:0;\">— " + escapeHtml(appName) + "</p>"
                + "</td></tr></table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    private String buildRejectedHtml(Invoice invoice, PaymentConfirmation confirmation) {
        String currency = invoice.getOrganization().getCurrency().getCurrencyCode();
        String customerName = invoice.getCustomer().getName();
        String orgName = invoice.getOrganization().getName();
        String appName = emailProperties.getFromName();

        String reasonBlock = confirmation.getBusinessNote() != null
                ? "<p style=\"background:#fef2f2;border-left:4px solid #dc2626;padding:12px 16px;"
                  + "border-radius:4px;color:#374151;margin:20px 0;font-size:14px;\">"
                  + "<strong>Reason:</strong><br>"
                  + escapeHtml(confirmation.getBusinessNote()) + "</p>"
                : "";

        return "<!DOCTYPE html>"
                + "<html><body style=\"margin:0;padding:0;background:#f4f4f5;font-family:Arial,sans-serif;\">"
                + "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\">"
                + "<tr><td align=\"center\" style=\"padding:40px 20px;\">"
                + "<table cellpadding=\"0\" cellspacing=\"0\" style=\"width:100%;max-width:560px;background:#ffffff;"
                + "border-radius:8px;padding:36px;box-shadow:0 1px 4px rgba(0,0,0,.08);\">"
                + "<tr><td style=\"font-size:15px;line-height:1.7;color:#374151;\">"
                + "<p style=\"margin:0 0 6px;font-size:13px;color:#6b7280;\">Invoice #" + escapeHtml(invoice.getInvoiceNumber()) + "</p>"
                + "<p style=\"margin:0 0 20px;font-size:18px;font-weight:bold;color:#dc2626;\">Payment Not Verified</p>"
                + "<p style=\"margin:0 0 16px;\">Hi <strong>" + escapeHtml(customerName) + "</strong>,</p>"
                + "<p style=\"margin:0 0 16px;\">Unfortunately, your payment confirmation for <strong>Invoice #" + escapeHtml(invoice.getInvoiceNumber()) + "</strong> could not be verified at this time.</p>"
                + reasonBlock
                + "<p style=\"margin:0 0 16px;\">The outstanding balance of <strong>" + escapeHtml(invoice.getRemainingAmount().toPlainString()) + " " + currency + "</strong> remains due."
                + " Please resubmit your confirmation or contact <strong>" + escapeHtml(orgName) + "</strong> for assistance.</p>"
                + "<hr style=\"border:none;border-top:1px solid #e5e7eb;margin:24px 0;\">"
                + "<p style=\"color:#9ca3af;font-size:12px;margin:0;\">— " + escapeHtml(appName) + "</p>"
                + "</td></tr></table>"
                + "</td></tr></table>"
                + "</body></html>";
    }

    // -----------------------------------------------------------------------
    // Sending helpers
    // -----------------------------------------------------------------------

    private void sendEmail(String from, String replyTo, String to, String subject, String html) {
        if (mailSender.isEmpty()) {
            log.info("Mail not configured — skipping notification to {} | subject: {}", to, subject);
            return;
        }
        try {
            MimeMessage message = mailSender.get().createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(emailProperties.getFromName() + " <" + from + ">");
            if (replyTo != null && !replyTo.isBlank()) {
                helper.setReplyTo(replyTo);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.get().send(message);
        } catch (Exception ex) {
            log.warn("Failed to send notification email to {}: {}", to, ex.getMessage());
        }
    }

    /** Minimal HTML escaping to prevent injection in user-supplied strings. */
    private static String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
