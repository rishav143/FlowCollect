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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.ObjectProvider;
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
        // EmailSender always uses MimeMessage — provide a real instance so MimeMessageHelper can use it
        when(javaMailSender.createMimeMessage()).thenReturn(new MimeMessage((Session) null));
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
    // send — plain text (no PDF) — always sends MimeMessage now
    // ===================================================================

    @Test
    void send_plainText_sendsMimeMessage_withCorrectSubjectAndRecipient() throws Exception {
        emailSender.send(customer, "Invoice #001 Due", "Please pay your invoice.");

        // A MimeMessage (not SimpleMailMessage) must be sent
        verify(javaMailSender).send(any(MimeMessage.class));

        // Capture the actual MimeMessage to inspect headers
        var captor = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertEquals("Invoice #001 Due", sent.getSubject());
        assertNotNull(sent.getRecipients(MimeMessage.RecipientType.TO));
        assertEquals("customer@example.com", sent.getRecipients(MimeMessage.RecipientType.TO)[0].toString());
    }

    @Test
    void send_usesDefaultSubject_whenSubjectIsBlank() throws Exception {
        emailSender.send(customer, "   ", "body");

        var captor = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertEquals("FlowCollect reminder", captor.getValue().getSubject());
    }

    @Test
    void send_usesDefaultSubject_whenSubjectIsNull() throws Exception {
        emailSender.send(customer, null, "body");

        var captor = org.mockito.ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        assertEquals("FlowCollect reminder", captor.getValue().getSubject());
    }

    @Test
    void send_treatsNullBody_asEmptyString() {
        // Should not throw; empty body is valid
        assertDoesNotThrow(() -> emailSender.send(customer, "Subject", null));
        verify(javaMailSender).send(any(MimeMessage.class));
    }

    @Test
    void send_withAttachPdfFalse_doesNotGeneratePdf_evenWhenInvoiceProvided() {
        emailSender.send(customer, "Subject", "Body", false, invoice);

        verify(javaMailSender).send(any(MimeMessage.class));
        verify(pdfGenerator, never()).generate(any());
    }

    @Test
    void send_withAttachPdfTrue_butNullInvoice_doesNotGeneratePdf() {
        emailSender.send(customer, "Subject", "Body", true, null);

        verify(javaMailSender).send(any(MimeMessage.class));
        verify(pdfGenerator, never()).generate(any());
    }

    // ===================================================================
    // send — with PDF attachment (MIME multipart)
    // ===================================================================

    @Test
    void send_withPdfAttachment_generatesPdfAndSendsMimeMessage() throws Exception {
        when(pdfGenerator.generate(invoice)).thenReturn(new byte[]{1, 2, 3});
        when(pdfGenerator.buildFileName(invoice)).thenReturn("INV-001.pdf");

        emailSender.send(customer, "Invoice #001", "Please see attached.", true, invoice);

        verify(javaMailSender).send(any(MimeMessage.class));
        verify(pdfGenerator).generate(invoice);
        verify(pdfGenerator).buildFileName(invoice);
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
        verify(javaMailSender, never()).send(any(MimeMessage.class));
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
                .when(javaMailSender).send(any(MimeMessage.class));

        InternalException ex = assertThrows(InternalException.class,
                () -> emailSender.send(customer, "Subject", "Body"));
        assertTrue(ex.getMessage().contains("Connection refused"));
    }
}
