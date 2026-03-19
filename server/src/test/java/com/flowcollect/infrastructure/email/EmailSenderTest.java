package com.flowcollect.infrastructure.email;

import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.followup.FollowUpChannel;
import com.flowcollect.exception.http.InternalException;
import com.flowcollect.infrastructure.config.NotificationEmailProperties;
import com.flowcollect.infrastructure.pdf.InvoicePdfGenerator;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailSenderTest {

    @Mock private JavaMailSender javaMailSender;
    @Mock private ObjectProvider<JavaMailSender> mailSenderProvider;
    @Mock private NotificationEmailProperties properties;
    @Mock private InvoicePdfGenerator pdfGenerator;
    @Mock private Customer customer;
    @Mock private Invoice invoice;

    private EmailSender emailSender;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mailSenderProvider.getIfAvailable()).thenReturn(javaMailSender);
        when(properties.getFromAddress()).thenReturn("billing@flowcollect.io");
        when(properties.getFromName()).thenReturn("FlowCollect");
        when(customer.getEmail()).thenReturn("customer@example.com");
        emailSender = new EmailSender(mailSenderProvider, properties, pdfGenerator);
    }

    // ===================================================================
    // channel()
    // ===================================================================

    @Test
    void channel_returnsEmail() {
        assertEquals(FollowUpChannel.EMAIL, emailSender.channel());
    }

    // ===================================================================
    // send — plain text (no PDF)
    // ===================================================================

    @Test
    void send_plainText_sendsSimpleMailMessage_withCorrectFields() {
        emailSender.send(customer, "Invoice #001 Due", "Please pay your invoice.");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertArrayEquals(new String[]{"customer@example.com"}, sent.getTo());
        assertEquals("billing@flowcollect.io", sent.getFrom());
        assertEquals("Invoice #001 Due", sent.getSubject());
        assertEquals("Please pay your invoice.", sent.getText());
    }

    @Test
    void send_usesDefaultSubject_whenSubjectIsBlank() {
        emailSender.send(customer, "   ", "body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertEquals("FlowCollect reminder", captor.getValue().getSubject());
    }

    @Test
    void send_usesDefaultSubject_whenSubjectIsNull() {
        emailSender.send(customer, null, "body");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertEquals("FlowCollect reminder", captor.getValue().getSubject());
    }

    @Test
    void send_treatsNullBody_asEmptyString() {
        emailSender.send(customer, "Subject", null);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertEquals("", captor.getValue().getText());
    }

    @Test
    void send_withAttachPdfFalse_sendsSimpleMessage_evenWhenInvoiceProvided() {
        emailSender.send(customer, "Subject", "Body", false, invoice);

        verify(javaMailSender).send(any(SimpleMailMessage.class));
        verify(javaMailSender, never()).createMimeMessage();
    }

    @Test
    void send_withAttachPdfTrue_butNullInvoice_sendsSimpleMessage() {
        emailSender.send(customer, "Subject", "Body", true, null);

        verify(javaMailSender).send(any(SimpleMailMessage.class));
        verify(javaMailSender, never()).createMimeMessage();
    }

    // ===================================================================
    // send — with PDF attachment (MIME)
    // ===================================================================

    @Test
    void send_withPdfAttachment_sendsMimeMessage_notSimpleMailMessage() throws Exception {
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(pdfGenerator.generate(invoice)).thenReturn(new byte[]{1, 2, 3});
        when(pdfGenerator.buildFileName(invoice)).thenReturn("INV-001.pdf");

        emailSender.send(customer, "Invoice #001", "Please see attached.", true, invoice);

        verify(javaMailSender).send(mimeMessage);
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    // ===================================================================
    // send — failure / misconfiguration
    // ===================================================================

    @Test
    void send_throwsInternalException_whenMailSenderNotConfigured() {
        when(mailSenderProvider.getIfAvailable()).thenReturn(null);
        EmailSender unconfigured = new EmailSender(mailSenderProvider, properties, pdfGenerator);

        assertThrows(InternalException.class,
                () -> unconfigured.send(customer, "Subject", "Body"));
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_throwsInternalException_whenCustomerEmailIsNull() {
        when(customer.getEmail()).thenReturn(null);

        assertThrows(InternalException.class,
                () -> emailSender.send(customer, "Subject", "Body"));
    }

    @Test
    void send_throwsInternalException_whenCustomerEmailIsBlank() {
        when(customer.getEmail()).thenReturn("  ");

        assertThrows(InternalException.class,
                () -> emailSender.send(customer, "Subject", "Body"));
        verify(javaMailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_throwsInternalException_whenFromAddressIsNull() {
        when(properties.getFromAddress()).thenReturn(null);

        assertThrows(InternalException.class,
                () -> emailSender.send(customer, "Subject", "Body"));
    }

    @Test
    void send_throwsInternalException_whenFromAddressIsBlank() {
        when(properties.getFromAddress()).thenReturn("");

        assertThrows(InternalException.class,
                () -> emailSender.send(customer, "Subject", "Body"));
    }

    @Test
    void send_wrapsSmtpException_asInternalException() {
        doThrow(new RuntimeException("Connection refused"))
                .when(javaMailSender).send(any(SimpleMailMessage.class));

        InternalException ex = assertThrows(InternalException.class,
                () -> emailSender.send(customer, "Subject", "Body"));
        assertTrue(ex.getMessage().contains("Connection refused"));
    }
}
