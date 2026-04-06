package com.flowcollect.infrastructure.email;

import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.followup.FollowUpChannel;
import com.flowcollect.exception.http.InternalException;
import com.flowcollect.infrastructure.config.NotificationEmailProperties;
import com.flowcollect.infrastructure.pdf.ConsolidatedSummaryPdfGenerator;
import com.flowcollect.infrastructure.pdf.InvoicePdfGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailSenderTest {

    @Mock private ResendEmailClient emailClient;
    @Mock private NotificationEmailProperties properties;
    @Mock private InvoicePdfGenerator pdfGenerator;
    @Mock private ConsolidatedSummaryPdfGenerator summaryPdfGenerator;
    @Mock private Customer customer;
    @Mock private Invoice invoice;

    private EmailSender emailSender;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(emailClient.isConfigured()).thenReturn(true);
        when(properties.getFromAddress()).thenReturn("billing@flowcollect.io");
        when(properties.getFromName()).thenReturn("FlowCollect");
        when(customer.getEmail()).thenReturn("customer@example.com");
        emailSender = new EmailSender(emailClient, properties, pdfGenerator, summaryPdfGenerator);
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
    void send_plainText_sendsEmail_withCorrectSubjectAndRecipient() {
        emailSender.send(customer, "Invoice #001 Due", "Please pay your invoice.");

        ArgumentCaptor<String> toCaptor      = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), toCaptor.capture(), subjectCaptor.capture(), anyString());
        assertEquals("customer@example.com", toCaptor.getValue());
        assertEquals("Invoice #001 Due", subjectCaptor.getValue());
    }

    @Test
    void send_usesDefaultSubject_whenSubjectIsBlank() {
        emailSender.send(customer, "   ", "body");

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), anyString(), subjectCaptor.capture(), anyString());
        assertEquals("FlowCollect reminder", subjectCaptor.getValue());
    }

    @Test
    void send_usesDefaultSubject_whenSubjectIsNull() {
        emailSender.send(customer, null, "body");

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), anyString(), subjectCaptor.capture(), anyString());
        assertEquals("FlowCollect reminder", subjectCaptor.getValue());
    }

    @Test
    void send_treatsNullBody_asEmptyString() {
        assertDoesNotThrow(() -> emailSender.send(customer, "Subject", null));
        verify(emailClient).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void send_withAttachPdfFalse_doesNotGeneratePdf_evenWhenInvoiceProvided() {
        emailSender.send(customer, "Subject", "Body", false, invoice);

        verify(emailClient).send(anyString(), anyString(), anyString(), anyString());
        verify(pdfGenerator, never()).generate(any());
    }

    @Test
    void send_withAttachPdfTrue_butNullInvoice_doesNotGeneratePdf() {
        emailSender.send(customer, "Subject", "Body", true, null);

        verify(emailClient).send(anyString(), anyString(), anyString(), anyString());
        verify(pdfGenerator, never()).generate(any());
    }

    // ===================================================================
    // send — with PDF attachment
    // ===================================================================

    @Test
    void send_withPdfAttachment_generatesPdfAndSendsWithAttachment() {
        when(pdfGenerator.generate(invoice)).thenReturn(new byte[]{1, 2, 3});
        when(pdfGenerator.buildFileName(invoice)).thenReturn("INV-001.pdf");

        emailSender.send(customer, "Invoice #001", "Please see attached.", true, invoice);

        verify(emailClient).send(anyString(), anyString(), anyString(), anyString(),
                eq("INV-001.pdf"), eq(new byte[]{1, 2, 3}));
        verify(pdfGenerator).generate(invoice);
        verify(pdfGenerator).buildFileName(invoice);
    }

    // ===================================================================
    // send — failure / misconfiguration
    // ===================================================================

    @Test
    void send_throwsInternalException_whenEmailClientNotConfigured() {
        when(emailClient.isConfigured()).thenReturn(false);

        assertThrows(InternalException.class,
                () -> emailSender.send(customer, "Subject", "Body"));
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
        verify(emailClient, never()).send(anyString(), anyString(), anyString(), anyString());
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
    void send_wrapsClientException_asInternalException() {
        doThrow(new RuntimeException("Connection refused"))
                .when(emailClient).send(anyString(), anyString(), anyString(), anyString());

        InternalException ex = assertThrows(InternalException.class,
                () -> emailSender.send(customer, "Subject", "Body"));
        assertTrue(ex.getMessage().contains("Connection refused"));
    }
}
