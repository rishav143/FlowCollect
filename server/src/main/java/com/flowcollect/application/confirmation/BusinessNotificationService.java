package com.flowcollect.application.confirmation;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import com.flowcollect.domain.confirmation.PaymentConfirmation;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.infrastructure.config.NotificationEmailProperties;

/**
 * Sends internal (business-facing) email notifications about payment confirmation events.
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
        String body = buildSubmittedBody(invoice, confirmation);
        sendToOrganization(invoice.getOrganization().getEmail(), subject, body);
    }

    /**
     * Notifies the customer that their payment confirmation was approved.
     */
    public void notifyCustomerApproved(Invoice invoice, PaymentConfirmation confirmation) {
        String customerEmail = invoice.getCustomer() != null ? invoice.getCustomer().getEmail() : null;
        if (customerEmail == null || customerEmail.isBlank()) {
            log.info("Skipping customer approval notification — no email on customer for invoice {}",
                    invoice.getId());
            return;
        }
        String currency = invoice.getOrganization().getCurrency().getCurrencyCode();
        String subject = "Payment Confirmed — Invoice #" + invoice.getInvoiceNumber();
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(invoice.getCustomer().getName()).append(",\n\n");
        sb.append("Your payment of ").append(confirmation.getAmountClaimed())
          .append(" ").append(currency)
          .append(" for Invoice #").append(invoice.getInvoiceNumber())
          .append(" has been confirmed. Thank you!\n");
        if (confirmation.getBusinessNote() != null) {
            sb.append("\nNote from ").append(invoice.getOrganization().getName())
              .append(": ").append(confirmation.getBusinessNote()).append("\n");
        }
        sb.append("\n— ").append(invoice.getOrganization().getName());
        sendToCustomer(customerEmail, invoice.getOrganization().getEmail(), subject, sb.toString());
    }

    /**
     * Notifies the customer that their payment confirmation was rejected.
     */
    public void notifyCustomerRejected(Invoice invoice, PaymentConfirmation confirmation) {
        String customerEmail = invoice.getCustomer() != null ? invoice.getCustomer().getEmail() : null;
        if (customerEmail == null || customerEmail.isBlank()) {
            log.info("Skipping customer rejection notification — no email on customer for invoice {}",
                    invoice.getId());
            return;
        }
        String currency = invoice.getOrganization().getCurrency().getCurrencyCode();
        String subject = "Payment Not Verified — Invoice #" + invoice.getInvoiceNumber();
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(invoice.getCustomer().getName()).append(",\n\n");
        sb.append("Your payment confirmation for Invoice #").append(invoice.getInvoiceNumber())
          .append(" of ").append(currency).append(" ").append(invoice.getRemainingAmount())
          .append(" could not be verified.\n");
        if (confirmation.getBusinessNote() != null) {
            sb.append("\nReason: ").append(confirmation.getBusinessNote()).append("\n");
        }
        sb.append("\nPlease resubmit your confirmation or contact ")
          .append(invoice.getOrganization().getName()).append(" for assistance.\n");
        sb.append("\n— ").append(invoice.getOrganization().getName());
        sendToCustomer(customerEmail, invoice.getOrganization().getEmail(), subject, sb.toString());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private String buildSubmittedBody(Invoice invoice, PaymentConfirmation confirmation) {
        String currency = invoice.getOrganization().getCurrency().getCurrencyCode();
        StringBuilder sb = new StringBuilder();
        sb.append("Hi,\n\n");
        sb.append("A customer has submitted a payment claim for Invoice #")
          .append(invoice.getInvoiceNumber()).append(".\n\n");
        sb.append("Claimed amount : ").append(confirmation.getAmountClaimed())
          .append(" ").append(currency).append("\n");
        sb.append("Remaining balance: ").append(invoice.getRemainingAmount())
          .append(" ").append(currency).append("\n");
        if (confirmation.getCustomerNote() != null) {
            sb.append("Customer note  : ").append(confirmation.getCustomerNote()).append("\n");
        }
        sb.append("\nPlease log in to review and approve or reject this claim.\n\n");
        sb.append("— ").append(emailProperties.getFromName());
        return sb.toString();
    }

    private void sendToOrganization(String toEmail, String subject, String body) {
        sendEmail(emailProperties.getFromName() + " <" + emailProperties.getFromAddress() + ">",
                toEmail, subject, body);
    }

    // Sends from the verified platform address; Reply-To is set to the org email
    // so the customer can reply directly to the business.
    private void sendToCustomer(String toEmail, String fromOrgEmail, String subject, String body) {
        if (mailSender.isEmpty()) {
            log.info("Mail not configured — skipping customer notification to {} | subject: {}", toEmail, subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailProperties.getFromName() + " <" + emailProperties.getFromAddress() + ">");
            message.setReplyTo(fromOrgEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.get().send(message);
        } catch (Exception ex) {
            log.warn("Failed to send customer notification email to {}: {}", toEmail, ex.getMessage());
        }
    }

    private void sendEmail(String from, String toEmail, String subject, String body) {
        if (mailSender.isEmpty()) {
            log.info("Mail not configured — skipping notification to {} | subject: {}", toEmail, subject);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.get().send(message);
        } catch (Exception ex) {
            log.warn("Failed to send notification email to {}: {}", toEmail, ex.getMessage());
        }
    }
}
