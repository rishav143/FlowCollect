package com.flowcollect.application.confirmation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.flowcollect.domain.confirmation.PaymentConfirmation;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.infrastructure.config.NotificationEmailProperties;
import com.flowcollect.infrastructure.email.EmailContent;
import com.flowcollect.infrastructure.email.ResendEmailClient;
import com.flowcollect.infrastructure.email.SystemEmailTemplates;

/**
 * Sends internal (business-facing) and customer-facing email notifications
 * for payment confirmation events.
 *
 * All methods are fire-and-forget: delivery failures are logged as warnings
 * and never propagate as exceptions.
 */
@Service
public class BusinessNotificationService {

    private static final Logger log = LoggerFactory.getLogger(BusinessNotificationService.class);

    private final ResendEmailClient emailClient;
    private final NotificationEmailProperties emailProperties;

    public BusinessNotificationService(
            ResendEmailClient emailClient,
            NotificationEmailProperties emailProperties
    ) {
        this.emailClient = emailClient;
        this.emailProperties = emailProperties;
    }

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
        if (!emailClient.isConfigured()) {
            log.info("Resend not configured — skipping notification to {} | subject: {}", to, content.subject());
            return;
        }
        try {
            String fromFormatted = emailProperties.getFromName() + " <" + from + ">";
            emailClient.send(fromFormatted, to, content.subject(), content.html());
        } catch (Exception ex) {
            log.warn("Failed to send notification email to {}: {}", to, ex.getMessage());
        }
    }
}
