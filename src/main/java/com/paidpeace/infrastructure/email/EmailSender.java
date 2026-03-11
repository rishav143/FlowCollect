package com.paidpeace.infrastructure.email;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.paidpeace.application.reminder.NotificationSender;
import com.paidpeace.domain.customer.Customer;
import com.paidpeace.domain.invoice.Invoice;
import com.paidpeace.domain.invoice.followup.FollowUpChannel;
import com.paidpeace.exception.http.InternalException;
import com.paidpeace.infrastructure.config.NotificationEmailProperties;
import com.paidpeace.infrastructure.pdf.InvoicePdfGenerator;

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
            if (attachPdf && invoice != null) {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);
                helper.setTo(recipient);
                helper.setFrom(fromAddress);
                helper.setSubject(emailSubject);
                helper.setText(emailBody);

                byte[] pdfBytes = pdfGenerator.generate(invoice);
                String fileName = pdfGenerator.buildFileName(invoice);
                helper.addAttachment(fileName, new ByteArrayResource(pdfBytes));

                mailSender.send(message);
                log.info("Sent EMAIL reminder with PDF attachment to {} with subject '{}'", recipient, emailSubject);
            } else {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(recipient);
                message.setFrom(fromAddress);
                message.setSubject(emailSubject);
                message.setText(emailBody);

                mailSender.send(message);
                log.info("Sent EMAIL reminder to {} with subject '{}'", recipient, emailSubject);
            }
        } catch (Exception ex) {
            throw new InternalException("Failed to send email reminder: " + ex.getMessage());
        }
    }

    private String requireConfigured(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new InternalException("Missing required email configuration: " + propertyName);
        }
        return value.trim();
    }
}
