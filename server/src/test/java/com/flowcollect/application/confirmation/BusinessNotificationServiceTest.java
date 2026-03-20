package com.flowcollect.application.confirmation;

import com.flowcollect.domain.confirmation.PaymentConfirmation;
import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.infrastructure.config.NotificationEmailProperties;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class BusinessNotificationServiceTest {

    @Mock private JavaMailSender javaMailSender;
    @Mock private NotificationEmailProperties emailProperties;
    @Mock private Invoice invoice;
    @Mock private Organization organization;
    @Mock private Customer customer;
    @Mock private PaymentConfirmation confirmation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(emailProperties.getFromAddress()).thenReturn("billing@flowcollect.io");
        when(emailProperties.getFromName()).thenReturn("FlowCollect");
        when(invoice.getOrganization()).thenReturn(organization);
        when(invoice.getInvoiceNumber()).thenReturn("INV-001");
        when(invoice.getRemainingAmount()).thenReturn(new BigDecimal("1000.00"));
        when(invoice.getCustomer()).thenReturn(customer);
        when(organization.getEmail()).thenReturn("biz@example.com");
        when(organization.getName()).thenReturn("Acme Corp");
        when(organization.getCurrency()).thenReturn(Currency.getInstance("INR"));
        when(customer.getEmail()).thenReturn("customer@example.com");
        when(customer.getName()).thenReturn("John");
        when(confirmation.getAmountClaimed()).thenReturn(new BigDecimal("1000.00"));
        when(confirmation.getCustomerNote()).thenReturn(null);
        when(confirmation.getBusinessNote()).thenReturn(null);
        // BusinessNotificationService always uses MimeMessage — provide a real instance
        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
    }

    private BusinessNotificationService serviceWith(JavaMailSender sender) {
        return new BusinessNotificationService(Optional.ofNullable(sender), emailProperties);
    }

    // ===================================================================
    // notifyPaymentSubmitted — happy path
    // ===================================================================

    @Test
    void notifyPaymentSubmitted_sendsEmail_toOrganization() throws Exception {
        BusinessNotificationService service = serviceWith(javaMailSender);

        service.notifyPaymentSubmitted(invoice, confirmation);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertNotNull(sent.getRecipients(MimeMessage.RecipientType.TO));
        assertEquals("biz@example.com",
                sent.getRecipients(MimeMessage.RecipientType.TO)[0].toString());
    }

    @Test
    void notifyPaymentSubmitted_subject_containsInvoiceNumber() throws Exception {
        BusinessNotificationService service = serviceWith(javaMailSender);

        service.notifyPaymentSubmitted(invoice, confirmation);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertEquals("Payment Claim Received — Invoice #INV-001", captor.getValue().getSubject());
    }

    @Test
    void notifyPaymentSubmitted_body_containsClaimedAmountAndCurrency() throws Exception {
        BusinessNotificationService service = serviceWith(javaMailSender);

        service.notifyPaymentSubmitted(invoice, confirmation);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        String body = captor.getValue().getContent().toString();
        assertNotNull(body);
        assertTrue(body.contains("1000.00"));
        assertTrue(body.contains("INR"));
        assertTrue(body.contains("INV-001"));
    }

    @Test
    void notifyPaymentSubmitted_body_showsBalanceAfterApproval() throws Exception {
        // remaining=1000, claimed=1000 → balance after approval = 0
        BusinessNotificationService service = serviceWith(javaMailSender);

        service.notifyPaymentSubmitted(invoice, confirmation);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        String body = captor.getValue().getContent().toString();
        assertTrue(body.contains("Balance after approval"));
        assertTrue(body.contains("0.00"));
    }

    @Test
    void notifyPaymentSubmitted_body_containsCustomerNote_whenPresent() throws Exception {
        when(confirmation.getCustomerNote()).thenReturn("Paid via NEFT ref 12345");
        BusinessNotificationService service = serviceWith(javaMailSender);

        service.notifyPaymentSubmitted(invoice, confirmation);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertTrue(captor.getValue().getContent().toString().contains("Paid via NEFT ref 12345"));
    }

    @Test
    void notifyPaymentSubmitted_body_omitsCustomerNoteField_whenNull() throws Exception {
        when(confirmation.getCustomerNote()).thenReturn(null);
        BusinessNotificationService service = serviceWith(javaMailSender);

        service.notifyPaymentSubmitted(invoice, confirmation);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertFalse(captor.getValue().getContent().toString().contains("Customer note"));
    }

    @Test
    void notifyPaymentSubmitted_from_containsConfiguredAddress() throws Exception {
        BusinessNotificationService service = serviceWith(javaMailSender);

        service.notifyPaymentSubmitted(invoice, confirmation);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertTrue(captor.getValue().getFrom()[0].toString().contains("billing@flowcollect.io"));
    }

    // ===================================================================
    // notifyPaymentSubmitted — fire-and-forget guarantees
    // ===================================================================

    @Test
    void notifyPaymentSubmitted_skipsEmail_whenMailSenderNotConfigured() {
        BusinessNotificationService service = serviceWith(null);

        assertDoesNotThrow(() -> service.notifyPaymentSubmitted(invoice, confirmation));
        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void notifyPaymentSubmitted_doesNotThrow_whenSmtpFails() {
        BusinessNotificationService service = serviceWith(javaMailSender);
        doThrow(new RuntimeException("SMTP unreachable"))
                .when(javaMailSender).send(any(MimeMessage.class));

        assertDoesNotThrow(() -> service.notifyPaymentSubmitted(invoice, confirmation));
    }

    // ===================================================================
    // notifyCustomerApproved
    // ===================================================================

    @Test
    void notifyCustomerApproved_sendsEmail_toCustomer() throws Exception {
        BusinessNotificationService service = serviceWith(javaMailSender);

        service.notifyCustomerApproved(invoice, confirmation);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertEquals("customer@example.com",
                captor.getValue().getRecipients(MimeMessage.RecipientType.TO)[0].toString());
    }

    @Test
    void notifyCustomerApproved_from_isPlatformAddress_replyTo_isOrgEmail() throws Exception {
        BusinessNotificationService service = serviceWith(javaMailSender);

        service.notifyCustomerApproved(invoice, confirmation);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertTrue(sent.getFrom()[0].toString().contains("billing@flowcollect.io"));
        assertEquals("biz@example.com", sent.getReplyTo()[0].toString());
    }

    @Test
    void notifyCustomerApproved_subject_containsInvoiceNumber() throws Exception {
        BusinessNotificationService service = serviceWith(javaMailSender);

        service.notifyCustomerApproved(invoice, confirmation);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertEquals("Payment Confirmed — Invoice #INV-001", captor.getValue().getSubject());
    }

    @Test
    void notifyCustomerApproved_body_containsAmountAndCurrency() throws Exception {
        BusinessNotificationService service = serviceWith(javaMailSender);

        service.notifyCustomerApproved(invoice, confirmation);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        String body = captor.getValue().getContent().toString();
        assertTrue(body.contains("1000.00"));
        assertTrue(body.contains("INR"));
        assertTrue(body.contains("INV-001"));
    }

    @Test
    void notifyCustomerApproved_body_containsBusinessNote_whenPresent() throws Exception {
        when(confirmation.getBusinessNote()).thenReturn("Verified via bank statement");
        BusinessNotificationService service = serviceWith(javaMailSender);

        service.notifyCustomerApproved(invoice, confirmation);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertTrue(captor.getValue().getContent().toString().contains("Verified via bank statement"));
    }

    @Test
    void notifyCustomerApproved_skips_whenCustomerHasNoEmail() {
        when(customer.getEmail()).thenReturn(null);
        BusinessNotificationService service = serviceWith(javaMailSender);

        assertDoesNotThrow(() -> service.notifyCustomerApproved(invoice, confirmation));
        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void notifyCustomerApproved_skips_whenMailNotConfigured() {
        BusinessNotificationService service = serviceWith(null);

        assertDoesNotThrow(() -> service.notifyCustomerApproved(invoice, confirmation));
        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    // ===================================================================
    // notifyCustomerRejected
    // ===================================================================

    @Test
    void notifyCustomerRejected_sendsEmail_toCustomer() throws Exception {
        BusinessNotificationService service = serviceWith(javaMailSender);

        service.notifyCustomerRejected(invoice, confirmation);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertEquals("customer@example.com",
                captor.getValue().getRecipients(MimeMessage.RecipientType.TO)[0].toString());
    }

    @Test
    void notifyCustomerRejected_from_isPlatformAddress_replyTo_isOrgEmail() throws Exception {
        BusinessNotificationService service = serviceWith(javaMailSender);

        service.notifyCustomerRejected(invoice, confirmation);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertTrue(sent.getFrom()[0].toString().contains("billing@flowcollect.io"));
        assertEquals("biz@example.com", sent.getReplyTo()[0].toString());
    }

    @Test
    void notifyCustomerRejected_subject_containsInvoiceNumber() throws Exception {
        BusinessNotificationService service = serviceWith(javaMailSender);

        service.notifyCustomerRejected(invoice, confirmation);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertEquals("Payment Not Verified — Invoice #INV-001", captor.getValue().getSubject());
    }

    @Test
    void notifyCustomerRejected_body_containsReasonAndOrgName() throws Exception {
        when(confirmation.getBusinessNote()).thenReturn("Amount does not match");
        BusinessNotificationService service = serviceWith(javaMailSender);

        service.notifyCustomerRejected(invoice, confirmation);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        String body = captor.getValue().getContent().toString();
        assertTrue(body.contains("Amount does not match"));
        assertTrue(body.contains("Acme Corp"));
    }

    @Test
    void notifyCustomerRejected_body_omitsReason_whenNoBusinessNote() throws Exception {
        when(confirmation.getBusinessNote()).thenReturn(null);
        BusinessNotificationService service = serviceWith(javaMailSender);

        service.notifyCustomerRejected(invoice, confirmation);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertFalse(captor.getValue().getContent().toString().contains("Reason:"));
    }

    @Test
    void notifyCustomerRejected_skips_whenCustomerHasNoEmail() {
        when(customer.getEmail()).thenReturn(null);
        BusinessNotificationService service = serviceWith(javaMailSender);

        assertDoesNotThrow(() -> service.notifyCustomerRejected(invoice, confirmation));
        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void notifyCustomerRejected_skips_whenMailNotConfigured() {
        BusinessNotificationService service = serviceWith(null);

        assertDoesNotThrow(() -> service.notifyCustomerRejected(invoice, confirmation));
        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }
}
