package com.flowcollect.application.confirmation;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.flowcollect.domain.confirmation.PaymentConfirmation;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.infrastructure.config.NotificationEmailProperties;
import com.flowcollect.infrastructure.email.EmailContent;
import com.flowcollect.infrastructure.email.SystemEmailTemplates;

import jakarta.mail.internet.MimeMessage;

/**
 * Sends internal (business-facing) and customer-facing email notifications
 * for payment confirmation events.
 *
 * <p>All email content is sourced from {@link SystemEmailTemplates} — edit that
 * class to change any message text or layout.
 *
 * <p>All methods are fire-and-forget: delivery failures are logged as warnings
 * and never propagate as exceptions, so a broken SMTP connection cannot block
 * the confirmation workflow.
 */
@Service
public class BusinessNotificationService {

    private static final Logger log = LoggerFactory.getLogger(BusinessNotificationService.class);

    private final Optional<JavaMailSender> mailSender;
    private final NotificationEmailProperties emailProperties;

    public BusinessNotificationService(
            Optional<JavaMailSender> mailSender,
            NotificationEmailProperties emailProperties
    ) {
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
        if (mailSender.isEmpty()) {
            log.warn("JavaMailSender is not configured — business notification emails will be skipped");
        }
    }

    // -----------------------------------------------------------------------
    // Business-facing notifications
    // -----------------------------------------------------------------------

    /**
     * Notifies the organization that a customer has submitted a new payment claim.
     */
    public void notifyPaymentSubmitted(Invoice invoice, PaymentConfirmation confirmation) {
        String customerName = invoice.getCustomer() != null ? invoice.getCustomer().getName() : null;
        EmailContent email = SystemEmailTemplates.paymentSubmittedToBusiness(
                invoice.getInvoiceNumber(),
                invoice.getOrganization().getName(),
                customerName,
                invoice.getOrganization().getCurrency().getCurrencyCode(),
                confirmation.getAmountClaimed(),
                invoice.getRemainingAmount(),
                confirmation.getCustomerNote(),
                emailProperties.getFromName()
        );
        sendEmail(emailProperties.getFromAddress(), null, invoice.getOrganization().getEmail(), email);
    }

    // -----------------------------------------------------------------------
    // Customer-facing notifications
    // -----------------------------------------------------------------------

    /**
     * Notifies the customer that their full payment was confirmed and the invoice
     * is now completely settled.
     *
     * <p>Call this when {@code isFullyPaid = true} after approval.
     */
    public void notifyCustomerPaymentConfirmedFull(Invoice invoice, PaymentConfirmation confirmation) {
        String customerEmail = resolveCustomerEmail(invoice);
        if (customerEmail == null) return;

        EmailContent email = SystemEmailTemplates.paymentConfirmedFull(
                invoice.getInvoiceNumber(),
                invoice.getCustomer().getName(),
                invoice.getOrganization().getName(),
                invoice.getOrganization().getCurrency().getCurrencyCode(),
                confirmation.getAmountClaimed(),
                confirmation.getBusinessNote(),
                emailProperties.getFromName()
        );
        sendEmail(emailProperties.getFromAddress(), invoice.getOrganization().getEmail(), customerEmail, email);
    }

    /**
     * Notifies the customer that their partial payment was approved but a balance remains.
     *
     * <p>Call this when approving a partial claim without explicitly requesting the remainder
     * (i.e. the standard "Approve" path when {@code isFullyPaid = false}).
     */
    public void notifyCustomerPartialPaymentApproved(Invoice invoice, PaymentConfirmation confirmation) {
        String customerEmail = resolveCustomerEmail(invoice);
        if (customerEmail == null) return;

        EmailContent email = SystemEmailTemplates.partialPaymentApproved(
                invoice.getInvoiceNumber(),
                invoice.getCustomer().getName(),
                invoice.getOrganization().getName(),
                invoice.getOrganization().getCurrency().getCurrencyCode(),
                confirmation.getAmountClaimed(),
                invoice.getRemainingAmount(),
                confirmation.getBusinessNote(),
                emailProperties.getFromName()
        );
        sendEmail(emailProperties.getFromAddress(), invoice.getOrganization().getEmail(), customerEmail, email);
    }

    /**
     * Notifies the customer that their partial payment was acknowledged AND explicitly
     * requests the remaining balance by the invoice due date.
     *
     * <p>This corresponds to the "Request Remaining" one-click action on the Approvals page.
     * No user input is needed — the message is fully system-generated.
     */
    public void notifyInstallmentRequest(Invoice invoice, PaymentConfirmation confirmation) {
        String customerEmail = resolveCustomerEmail(invoice);
        if (customerEmail == null) return;

        EmailContent email = SystemEmailTemplates.installmentRequest(
                invoice.getInvoiceNumber(),
                invoice.getCustomer().getName(),
                invoice.getOrganization().getName(),
                invoice.getOrganization().getCurrency().getCurrencyCode(),
                confirmation.getAmountClaimed(),
                invoice.getRemainingAmount(),
                invoice.getDueDate(),
                emailProperties.getFromName()
        );
        sendEmail(emailProperties.getFromAddress(), invoice.getOrganization().getEmail(), customerEmail, email);
    }

    /**
     * Notifies the customer that their payment claim was rejected and the balance remains due.
     */
    public void notifyCustomerPaymentRejected(Invoice invoice, PaymentConfirmation confirmation) {
        String customerEmail = resolveCustomerEmail(invoice);
        if (customerEmail == null) return;

        EmailContent email = SystemEmailTemplates.paymentRejected(
                invoice.getInvoiceNumber(),
                invoice.getCustomer().getName(),
                invoice.getOrganization().getName(),
                invoice.getOrganization().getCurrency().getCurrencyCode(),
                invoice.getRemainingAmount(),
                confirmation.getBusinessNote(),
                emailProperties.getFromName()
        );
        sendEmail(emailProperties.getFromAddress(), invoice.getOrganization().getEmail(), customerEmail, email);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    /**
     * Returns the customer's email address, or {@code null} with a log warning if absent.
     * Callers must skip notification when this returns {@code null}.
     */
    private String resolveCustomerEmail(Invoice invoice) {
        String email = invoice.getCustomer() != null ? invoice.getCustomer().getEmail() : null;
        if (email == null || email.isBlank()) {
            log.info("Skipping customer notification — no email on customer for invoice [invoiceId={}]",
                    invoice.getId());
            return null;
        }
        return email;
    }

    private void sendEmail(String from, String replyTo, String to, EmailContent content) {
        if (mailSender.isEmpty()) {
            log.info("Mail not configured — skipping notification to {} | subject: {}", to, content.subject());
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
            helper.setSubject(content.subject());
            helper.setText(content.html(), true);
            mailSender.get().send(message);
        } catch (Exception ex) {
            log.warn("Failed to send notification email to {}: {}", to, ex.getMessage());
        }
    }
}
