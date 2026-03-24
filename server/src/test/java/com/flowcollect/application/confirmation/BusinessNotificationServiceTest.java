package com.flowcollect.application.confirmation;

import com.flowcollect.domain.confirmation.PaymentConfirmation;
import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.infrastructure.config.NotificationEmailProperties;
import com.flowcollect.infrastructure.email.ResendEmailClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BusinessNotificationServiceTest {

    @Mock private ResendEmailClient emailClient;
    @Mock private NotificationEmailProperties emailProperties;
    @Mock private Invoice invoice;
    @Mock private Organization organization;
    @Mock private Customer customer;
    @Mock private PaymentConfirmation confirmation;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(emailClient.isConfigured()).thenReturn(true);
        when(emailProperties.getFromAddress()).thenReturn("billing@flowcollect.io");
        when(emailProperties.getFromName()).thenReturn("FlowCollect");
        when(invoice.getOrganization()).thenReturn(organization);
        when(invoice.getInvoiceNumber()).thenReturn("INV-001");
        when(invoice.getRemainingAmount()).thenReturn(new BigDecimal("1000.00"));
        when(invoice.getDueDate()).thenReturn(LocalDate.of(2026, 3, 31));
        when(invoice.getCustomer()).thenReturn(customer);
        when(organization.getEmail()).thenReturn("biz@example.com");
        when(organization.getName()).thenReturn("Acme Corp");
        when(organization.getCurrency()).thenReturn(Currency.getInstance("INR"));
        when(customer.getEmail()).thenReturn("customer@example.com");
        when(customer.getName()).thenReturn("John");
        when(confirmation.getAmountClaimed()).thenReturn(new BigDecimal("1000.00"));
        when(confirmation.getCustomerNote()).thenReturn(null);
        when(confirmation.getBusinessNote()).thenReturn(null);
    }

    private BusinessNotificationService service() {
        return new BusinessNotificationService(emailClient, emailProperties);
    }

    // ===================================================================
    // notifyPaymentSubmitted — happy path
    // ===================================================================

    @Test
    void notifyPaymentSubmitted_sendsEmail_toOrganization() {
        service().notifyPaymentSubmitted(invoice, confirmation);

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), toCaptor.capture(), anyString(), anyString());
        assertEquals("biz@example.com", toCaptor.getValue());
    }

    @Test
    void notifyPaymentSubmitted_subject_containsInvoiceNumber() {
        service().notifyPaymentSubmitted(invoice, confirmation);

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), anyString(), subjectCaptor.capture(), anyString());
        assertEquals("Payment Claim Received — Invoice #INV-001", subjectCaptor.getValue());
    }

    @Test
    void notifyPaymentSubmitted_body_containsClaimedAmountAndCurrency() {
        service().notifyPaymentSubmitted(invoice, confirmation);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), anyString(), anyString(), htmlCaptor.capture());
        String html = htmlCaptor.getValue();
        assertTrue(html.contains("1000.00"));
        assertTrue(html.contains("INR"));
        assertTrue(html.contains("INV-001"));
    }

    @Test
    void notifyPaymentSubmitted_body_showsBalanceAfterApproval() {
        service().notifyPaymentSubmitted(invoice, confirmation);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), anyString(), anyString(), htmlCaptor.capture());
        String html = htmlCaptor.getValue();
        assertTrue(html.contains("Balance after approval") || html.contains("0.00"));
    }

    @Test
    void notifyPaymentSubmitted_body_containsCustomerNote_whenPresent() {
        when(confirmation.getCustomerNote()).thenReturn("Paid via NEFT ref 12345");
        service().notifyPaymentSubmitted(invoice, confirmation);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), anyString(), anyString(), htmlCaptor.capture());
        assertTrue(htmlCaptor.getValue().contains("Paid via NEFT ref 12345"));
    }

    @Test
    void notifyPaymentSubmitted_body_omitsCustomerNoteField_whenNull() {
        when(confirmation.getCustomerNote()).thenReturn(null);
        service().notifyPaymentSubmitted(invoice, confirmation);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), anyString(), anyString(), htmlCaptor.capture());
        assertFalse(htmlCaptor.getValue().contains("Customer note"));
    }

    @Test
    void notifyPaymentSubmitted_from_containsConfiguredAddress() {
        service().notifyPaymentSubmitted(invoice, confirmation);

        ArgumentCaptor<String> fromCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(fromCaptor.capture(), anyString(), anyString(), anyString());
        assertTrue(fromCaptor.getValue().contains("billing@flowcollect.io"));
    }

    // ===================================================================
    // notifyPaymentSubmitted — fire-and-forget guarantees
    // ===================================================================

    @Test
    void notifyPaymentSubmitted_skipsEmail_whenClientNotConfigured() {
        when(emailClient.isConfigured()).thenReturn(false);

        assertDoesNotThrow(() -> service().notifyPaymentSubmitted(invoice, confirmation));
        verify(emailClient, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void notifyPaymentSubmitted_doesNotThrow_whenClientFails() {
        doThrow(new RuntimeException("API error"))
                .when(emailClient).send(anyString(), anyString(), anyString(), anyString());

        assertDoesNotThrow(() -> service().notifyPaymentSubmitted(invoice, confirmation));
    }

    // ===================================================================
    // notifyCustomerPaymentConfirmedFull
    // ===================================================================

    @Test
    void notifyCustomerPaymentConfirmedFull_sendsEmail_toCustomer() {
        service().notifyCustomerPaymentConfirmedFull(invoice, confirmation);

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), toCaptor.capture(), anyString(), anyString());
        assertEquals("customer@example.com", toCaptor.getValue());
    }

    @Test
    void notifyCustomerPaymentConfirmedFull_subject_containsInvoiceNumber() {
        service().notifyCustomerPaymentConfirmedFull(invoice, confirmation);

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), anyString(), subjectCaptor.capture(), anyString());
        assertEquals("Payment Confirmed — Invoice #INV-001", subjectCaptor.getValue());
    }

    @Test
    void notifyCustomerPaymentConfirmedFull_body_containsAmountAndCurrency() {
        service().notifyCustomerPaymentConfirmedFull(invoice, confirmation);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), anyString(), anyString(), htmlCaptor.capture());
        String html = htmlCaptor.getValue();
        assertTrue(html.contains("1000.00"));
        assertTrue(html.contains("INR"));
        assertTrue(html.contains("INV-001"));
    }

    @Test
    void notifyCustomerPaymentConfirmedFull_body_containsBusinessNote_whenPresent() {
        when(confirmation.getBusinessNote()).thenReturn("Verified via bank statement");
        service().notifyCustomerPaymentConfirmedFull(invoice, confirmation);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), anyString(), anyString(), htmlCaptor.capture());
        assertTrue(htmlCaptor.getValue().contains("Verified via bank statement"));
    }

    @Test
    void notifyCustomerPaymentConfirmedFull_skips_whenCustomerHasNoEmail() {
        when(customer.getEmail()).thenReturn(null);

        assertDoesNotThrow(() -> service().notifyCustomerPaymentConfirmedFull(invoice, confirmation));
        verify(emailClient, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void notifyCustomerPaymentConfirmedFull_skips_whenClientNotConfigured() {
        when(emailClient.isConfigured()).thenReturn(false);

        assertDoesNotThrow(() -> service().notifyCustomerPaymentConfirmedFull(invoice, confirmation));
        verify(emailClient, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    // ===================================================================
    // notifyCustomerPartialPaymentApproved
    // ===================================================================

    @Test
    void notifyCustomerPartialPaymentApproved_sendsEmail_toCustomer() {
        when(confirmation.getAmountClaimed()).thenReturn(new BigDecimal("400.00"));
        when(invoice.getRemainingAmount()).thenReturn(new BigDecimal("600.00"));

        service().notifyCustomerPartialPaymentApproved(invoice, confirmation);

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), toCaptor.capture(), anyString(), anyString());
        assertEquals("customer@example.com", toCaptor.getValue());
    }

    @Test
    void notifyCustomerPartialPaymentApproved_subject_indicatesPartial() {
        when(confirmation.getAmountClaimed()).thenReturn(new BigDecimal("400.00"));
        when(invoice.getRemainingAmount()).thenReturn(new BigDecimal("600.00"));

        service().notifyCustomerPartialPaymentApproved(invoice, confirmation);

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), anyString(), subjectCaptor.capture(), anyString());
        assertEquals("Partial Payment Received — Invoice #INV-001", subjectCaptor.getValue());
    }

    @Test
    void notifyCustomerPartialPaymentApproved_body_showsAmountAndRemainingBalance() {
        when(confirmation.getAmountClaimed()).thenReturn(new BigDecimal("400.00"));
        when(invoice.getRemainingAmount()).thenReturn(new BigDecimal("600.00"));

        service().notifyCustomerPartialPaymentApproved(invoice, confirmation);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), anyString(), anyString(), htmlCaptor.capture());
        String html = htmlCaptor.getValue();
        assertTrue(html.contains("400.00"));
        assertTrue(html.contains("600.00"));
        assertTrue(html.contains("INR"));
    }

    @Test
    void notifyCustomerPartialPaymentApproved_skips_whenCustomerHasNoEmail() {
        when(customer.getEmail()).thenReturn(null);

        assertDoesNotThrow(() -> service().notifyCustomerPartialPaymentApproved(invoice, confirmation));
        verify(emailClient, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    // ===================================================================
    // notifyInstallmentRequest
    // ===================================================================

    @Test
    void notifyInstallmentRequest_sendsEmail_toCustomer() {
        when(confirmation.getAmountClaimed()).thenReturn(new BigDecimal("300.00"));
        when(invoice.getRemainingAmount()).thenReturn(new BigDecimal("700.00"));

        service().notifyInstallmentRequest(invoice, confirmation);

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), toCaptor.capture(), anyString(), anyString());
        assertEquals("customer@example.com", toCaptor.getValue());
    }

    @Test
    void notifyInstallmentRequest_subject_indicatesRemainingBalance() {
        when(confirmation.getAmountClaimed()).thenReturn(new BigDecimal("300.00"));
        when(invoice.getRemainingAmount()).thenReturn(new BigDecimal("700.00"));

        service().notifyInstallmentRequest(invoice, confirmation);

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), anyString(), subjectCaptor.capture(), anyString());
        assertEquals("Payment Request for Remaining Balance — Invoice #INV-001", subjectCaptor.getValue());
    }

    @Test
    void notifyInstallmentRequest_body_showsAmountReceivedRemainingAndDueDate() {
        when(confirmation.getAmountClaimed()).thenReturn(new BigDecimal("300.00"));
        when(invoice.getRemainingAmount()).thenReturn(new BigDecimal("700.00"));

        service().notifyInstallmentRequest(invoice, confirmation);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), anyString(), anyString(), htmlCaptor.capture());
        String html = htmlCaptor.getValue();
        assertTrue(html.contains("300.00"));
        assertTrue(html.contains("700.00"));
        assertTrue(html.contains("31 Mar 2026"));
        assertTrue(html.contains("INR"));
    }

    @Test
    void notifyInstallmentRequest_skips_whenCustomerHasNoEmail() {
        when(customer.getEmail()).thenReturn(null);

        assertDoesNotThrow(() -> service().notifyInstallmentRequest(invoice, confirmation));
        verify(emailClient, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void notifyInstallmentRequest_skips_whenClientNotConfigured() {
        when(emailClient.isConfigured()).thenReturn(false);

        assertDoesNotThrow(() -> service().notifyInstallmentRequest(invoice, confirmation));
        verify(emailClient, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    // ===================================================================
    // notifyCustomerPaymentRejected
    // ===================================================================

    @Test
    void notifyCustomerPaymentRejected_sendsEmail_toCustomer() {
        service().notifyCustomerPaymentRejected(invoice, confirmation);

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), toCaptor.capture(), anyString(), anyString());
        assertEquals("customer@example.com", toCaptor.getValue());
    }

    @Test
    void notifyCustomerPaymentRejected_subject_containsInvoiceNumber() {
        service().notifyCustomerPaymentRejected(invoice, confirmation);

        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), anyString(), subjectCaptor.capture(), anyString());
        assertEquals("Payment Not Verified — Invoice #INV-001", subjectCaptor.getValue());
    }

    @Test
    void notifyCustomerPaymentRejected_body_containsReasonAndOrgName() {
        when(confirmation.getBusinessNote()).thenReturn("Amount does not match");

        service().notifyCustomerPaymentRejected(invoice, confirmation);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), anyString(), anyString(), htmlCaptor.capture());
        String html = htmlCaptor.getValue();
        assertTrue(html.contains("Amount does not match"));
        assertTrue(html.contains("Acme Corp"));
    }

    @Test
    void notifyCustomerPaymentRejected_body_omitsReason_whenNoBusinessNote() {
        when(confirmation.getBusinessNote()).thenReturn(null);

        service().notifyCustomerPaymentRejected(invoice, confirmation);

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailClient).send(anyString(), anyString(), anyString(), htmlCaptor.capture());
        assertFalse(htmlCaptor.getValue().contains("Reason:"));
    }

    @Test
    void notifyCustomerPaymentRejected_skips_whenCustomerHasNoEmail() {
        when(customer.getEmail()).thenReturn(null);

        assertDoesNotThrow(() -> service().notifyCustomerPaymentRejected(invoice, confirmation));
        verify(emailClient, never()).send(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void notifyCustomerPaymentRejected_skips_whenClientNotConfigured() {
        when(emailClient.isConfigured()).thenReturn(false);

        assertDoesNotThrow(() -> service().notifyCustomerPaymentRejected(invoice, confirmation));
        verify(emailClient, never()).send(anyString(), anyString(), anyString(), anyString());
    }
}
