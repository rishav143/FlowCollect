package com.flowcollect.application.confirmation;

import com.flowcollect.api.v1.confirmation.dto.CustomerPaymentSubmitRequest;
import com.flowcollect.api.v1.confirmation.dto.ReviewConfirmationRequest;
import com.flowcollect.application.invoice.InvoiceService;
import com.flowcollect.application.invoice.PaymentService;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.domain.confirmation.ConfirmationLink;
import com.flowcollect.domain.confirmation.PaymentConfirmation;
import com.flowcollect.domain.confirmation.PaymentConfirmationStatus;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.payment.PaymentMode;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.exception.http.ConflictException;
import com.flowcollect.exception.http.NotFoundException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.confirmation.PaymentConfirmationJpaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class PaymentConfirmationServiceTest {

    @Mock private PaymentConfirmationJpaRepository confirmationRepository;
    @Mock private ConfirmationLinkService confirmationLinkService;
    @Mock private PaymentService paymentService;
    @Mock private InvoiceService invoiceService;
    @Mock private OrganizationService organizationService;
    @Mock private BusinessNotificationService businessNotificationService;

    private PaymentConfirmationService service;

    // Test fixtures
    private final UUID orgId          = UUID.randomUUID();
    private final UUID invoiceId      = UUID.randomUUID();
    private final UUID confirmationId = UUID.randomUUID();
    private final String token        = "abc123def456abc123def456abc12345";

    @Mock private Organization organization;
    @Mock private Invoice invoice;
    @Mock private ConfirmationLink confirmationLink;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PaymentConfirmationService(
                confirmationRepository, confirmationLinkService, paymentService,
                invoiceService, organizationService, businessNotificationService);

        // Common stubs
        when(organization.getId()).thenReturn(orgId);
        when(organization.getCurrency()).thenReturn(Currency.getInstance("INR"));
        when(invoice.getId()).thenReturn(invoiceId);
        when(invoice.getOrganization()).thenReturn(organization);
        when(invoice.getInvoiceNumber()).thenReturn("INV-001");
        when(invoice.getTotalAmount()).thenReturn(new BigDecimal("1000.00"));
        when(invoice.getTotalPaid()).thenReturn(BigDecimal.ZERO);
        when(invoice.getRemainingAmount()).thenReturn(new BigDecimal("1000.00"));
        when(invoice.isIssued()).thenReturn(true);
        when(invoice.isPartiallyPaid()).thenReturn(false);

        when(confirmationLink.getId()).thenReturn(UUID.randomUUID());
        when(confirmationLink.getInvoice()).thenReturn(invoice);
        when(confirmationLink.isOpen()).thenReturn(true);
    }

    // ===================================================================
    // submitByCustomer — happy path
    // ===================================================================

    @Test
    void submit_fullAmount_succeeds() {
        CustomerPaymentSubmitRequest request = new CustomerPaymentSubmitRequest();
        request.setAmountClaimed(new BigDecimal("1000.00"));

        when(confirmationLinkService.getByToken(token)).thenReturn(confirmationLink);
        when(confirmationRepository.existsByConfirmationLinkIdAndStatus(
                any(), eq(PaymentConfirmationStatus.PENDING_APPROVAL))).thenReturn(false);
        when(confirmationRepository.save(any(PaymentConfirmation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentConfirmation result = service.submitByCustomer(token, request);

        assertNotNull(result);
        assertEquals(new BigDecimal("1000.00"), result.getAmountClaimed());
        assertEquals(PaymentConfirmationStatus.PENDING_APPROVAL, result.getStatus());
        verify(businessNotificationService).notifyPaymentSubmitted(invoice, result);
    }

    @Test
    void submit_partialAmount_succeeds() {
        CustomerPaymentSubmitRequest request = new CustomerPaymentSubmitRequest();
        request.setAmountClaimed(new BigDecimal("400.00"));
        request.setCustomerNote("Paid half now, rest next week");

        when(confirmationLinkService.getByToken(token)).thenReturn(confirmationLink);
        when(confirmationRepository.existsByConfirmationLinkIdAndStatus(any(), any())).thenReturn(false);
        when(confirmationRepository.save(any(PaymentConfirmation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentConfirmation result = service.submitByCustomer(token, request);

        assertEquals(new BigDecimal("400.00"), result.getAmountClaimed());
        assertEquals("Paid half now, rest next week", result.getCustomerNote());
    }

    @Test
    void submit_tripsCustomerNote_whitespace() {
        CustomerPaymentSubmitRequest request = new CustomerPaymentSubmitRequest();
        request.setAmountClaimed(new BigDecimal("500.00"));
        request.setCustomerNote("  NEFT ref 123  ");

        when(confirmationLinkService.getByToken(token)).thenReturn(confirmationLink);
        when(confirmationRepository.existsByConfirmationLinkIdAndStatus(any(), any())).thenReturn(false);
        when(confirmationRepository.save(any(PaymentConfirmation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        PaymentConfirmation result = service.submitByCustomer(token, request);

        assertEquals("NEFT ref 123", result.getCustomerNote());
    }

    // ===================================================================
    // submitByCustomer — edge cases & failures
    // ===================================================================

    @Test
    void submit_fails_whenRequestIsNull() {
        assertThrows(ValidationException.class,
                () -> service.submitByCustomer(token, null));
    }

    @Test
    void submit_fails_whenLinkIsClosed() {
        when(confirmationLinkService.getByToken(token)).thenReturn(confirmationLink);
        when(confirmationLink.isOpen()).thenReturn(false);

        CustomerPaymentSubmitRequest request = new CustomerPaymentSubmitRequest();
        request.setAmountClaimed(new BigDecimal("1000.00"));

        assertThrows(ConflictException.class,
                () -> service.submitByCustomer(token, request));
        verify(confirmationRepository, never()).save(any());
    }

    @Test
    void submit_fails_whenInvoiceIsNotCollectible() {
        when(confirmationLinkService.getByToken(token)).thenReturn(confirmationLink);
        when(invoice.isIssued()).thenReturn(false);
        when(invoice.isPartiallyPaid()).thenReturn(false);

        CustomerPaymentSubmitRequest request = new CustomerPaymentSubmitRequest();
        request.setAmountClaimed(new BigDecimal("1000.00"));

        assertThrows(ConflictException.class,
                () -> service.submitByCustomer(token, request));
    }

    @Test
    void submit_fails_whenAnotherSubmissionAlreadyPending() {
        when(confirmationLinkService.getByToken(token)).thenReturn(confirmationLink);
        when(confirmationRepository.existsByConfirmationLinkIdAndStatus(
                any(), eq(PaymentConfirmationStatus.PENDING_APPROVAL))).thenReturn(true);

        CustomerPaymentSubmitRequest request = new CustomerPaymentSubmitRequest();
        request.setAmountClaimed(new BigDecimal("1000.00"));

        assertThrows(ConflictException.class,
                () -> service.submitByCustomer(token, request));
        verify(confirmationRepository, never()).save(any());
    }

    @Test
    void submit_fails_whenAmountIsZero() {
        when(confirmationLinkService.getByToken(token)).thenReturn(confirmationLink);
        when(confirmationRepository.existsByConfirmationLinkIdAndStatus(any(), any())).thenReturn(false);

        CustomerPaymentSubmitRequest request = new CustomerPaymentSubmitRequest();
        request.setAmountClaimed(BigDecimal.ZERO);

        assertThrows(ValidationException.class,
                () -> service.submitByCustomer(token, request));
    }

    @Test
    void submit_fails_whenAmountIsNegative() {
        when(confirmationLinkService.getByToken(token)).thenReturn(confirmationLink);
        when(confirmationRepository.existsByConfirmationLinkIdAndStatus(any(), any())).thenReturn(false);

        CustomerPaymentSubmitRequest request = new CustomerPaymentSubmitRequest();
        request.setAmountClaimed(new BigDecimal("-100.00"));

        assertThrows(ValidationException.class,
                () -> service.submitByCustomer(token, request));
    }

    @Test
    void submit_fails_whenAmountIsNull() {
        when(confirmationLinkService.getByToken(token)).thenReturn(confirmationLink);
        when(confirmationRepository.existsByConfirmationLinkIdAndStatus(any(), any())).thenReturn(false);

        CustomerPaymentSubmitRequest request = new CustomerPaymentSubmitRequest();
        request.setAmountClaimed(null);

        assertThrows(ValidationException.class,
                () -> service.submitByCustomer(token, request));
    }

    @Test
    void submit_fails_whenAmountExceedsRemainingBalance() {
        when(invoice.getRemainingAmount()).thenReturn(new BigDecimal("500.00"));
        when(confirmationLinkService.getByToken(token)).thenReturn(confirmationLink);
        when(confirmationRepository.existsByConfirmationLinkIdAndStatus(any(), any())).thenReturn(false);

        CustomerPaymentSubmitRequest request = new CustomerPaymentSubmitRequest();
        request.setAmountClaimed(new BigDecimal("501.00")); // 1 rupee over

        ValidationException ex = assertThrows(ValidationException.class,
                () -> service.submitByCustomer(token, request));
        assertTrue(ex.getMessage().contains("exceeds the invoice remaining balance"));
    }

    @Test
    void submit_succeeds_whenAmountEqualsExactRemaining() {
        when(invoice.getRemainingAmount()).thenReturn(new BigDecimal("250.00"));
        when(confirmationLinkService.getByToken(token)).thenReturn(confirmationLink);
        when(confirmationRepository.existsByConfirmationLinkIdAndStatus(any(), any())).thenReturn(false);
        when(confirmationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CustomerPaymentSubmitRequest request = new CustomerPaymentSubmitRequest();
        request.setAmountClaimed(new BigDecimal("250.00"));

        assertDoesNotThrow(() -> service.submitByCustomer(token, request));
    }

    @Test
    void submit_notificationFailure_doesNotRollBack() {
        when(confirmationLinkService.getByToken(token)).thenReturn(confirmationLink);
        when(confirmationRepository.existsByConfirmationLinkIdAndStatus(any(), any())).thenReturn(false);
        when(confirmationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // Simulate notification service throwing a runtime exception
        doThrow(new RuntimeException("SMTP unreachable"))
                .when(businessNotificationService).notifyPaymentSubmitted(any(), any());

        CustomerPaymentSubmitRequest request = new CustomerPaymentSubmitRequest();
        request.setAmountClaimed(new BigDecimal("1000.00"));

        // BusinessNotificationService catches internally — exception must not propagate
        assertDoesNotThrow(() -> service.submitByCustomer(token, request));
    }

    // ===================================================================
    // approveByBusiness — happy path
    // ===================================================================

    @Test
    void approve_recordsPaymentAndClosesLink_whenInvoiceBecomesFullyPaid() {
        PaymentConfirmation pendingConfirmation = buildPendingConfirmation(new BigDecimal("1000.00"));

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(pendingConfirmation));
        when(confirmationRepository.save(pendingConfirmation)).thenReturn(pendingConfirmation);

        // After payment is recorded, the reloaded invoice is PAID
        Invoice paidInvoice = mock(Invoice.class);
        when(paidInvoice.isPaid()).thenReturn(true);
        when(invoiceService.getInvoiceById(invoiceId)).thenReturn(paidInvoice);

        ReviewConfirmationRequest reviewRequest = new ReviewConfirmationRequest();
        reviewRequest.setNote("Confirmed via bank statement");

        PaymentConfirmation result = service.approveByBusiness(orgId, confirmationId, reviewRequest);

        assertEquals(PaymentConfirmationStatus.APPROVED, result.getStatus());
        assertEquals("Confirmed via bank statement", result.getBusinessNote());

        // Verify payment was recorded with BANK_TRANSFER mode
        ArgumentCaptor<PaymentMode> modeCaptor = ArgumentCaptor.forClass(PaymentMode.class);
        verify(paymentService).recordGatewayPayment(
                eq(invoiceId), eq(new BigDecimal("1000.00")), modeCaptor.capture(), any(), any());
        assertEquals(PaymentMode.BANK_TRANSFER, modeCaptor.getValue());

        // Verify link is closed because invoice is now fully paid
        verify(confirmationLinkService).closeForInvoice(invoiceId);
    }

    @Test
    void approve_doesNotCloseLink_whenInvoiceIsPartiallyPaid() {
        PaymentConfirmation pendingConfirmation = buildPendingConfirmation(new BigDecimal("300.00"));

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(pendingConfirmation));
        when(confirmationRepository.save(pendingConfirmation)).thenReturn(pendingConfirmation);

        Invoice partiallyPaidInvoice = mock(Invoice.class);
        when(partiallyPaidInvoice.isPaid()).thenReturn(false); // still outstanding balance
        when(invoiceService.getInvoiceById(invoiceId)).thenReturn(partiallyPaidInvoice);

        service.approveByBusiness(orgId, confirmationId, null);

        verify(confirmationLinkService, never()).closeForInvoice(any());
    }

    @Test
    void approve_withNullNote_setsNullBusinessNote() {
        PaymentConfirmation pendingConfirmation = buildPendingConfirmation(new BigDecimal("1000.00"));

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(pendingConfirmation));
        when(confirmationRepository.save(pendingConfirmation)).thenReturn(pendingConfirmation);
        when(invoiceService.getInvoiceById(invoiceId)).thenReturn(mock(Invoice.class));

        service.approveByBusiness(orgId, confirmationId, null);

        assertNull(pendingConfirmation.getBusinessNote());
    }

    @Test
    void approve_withBlankNote_treatsAsNull() {
        PaymentConfirmation pendingConfirmation = buildPendingConfirmation(new BigDecimal("1000.00"));

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(pendingConfirmation));
        when(confirmationRepository.save(pendingConfirmation)).thenReturn(pendingConfirmation);
        when(invoiceService.getInvoiceById(invoiceId)).thenReturn(mock(Invoice.class));

        ReviewConfirmationRequest reviewRequest = new ReviewConfirmationRequest();
        reviewRequest.setNote("   ");

        service.approveByBusiness(orgId, confirmationId, reviewRequest);

        assertNull(pendingConfirmation.getBusinessNote());
    }

    // ===================================================================
    // approveByBusiness — failures
    // ===================================================================

    @Test
    void approve_fails_whenConfirmationNotFound() {
        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.approveByBusiness(orgId, confirmationId, null));
    }

    @Test
    void approve_fails_whenConfirmationBelongsToDifferentOrg() {
        UUID differentOrgId = UUID.randomUUID();
        Organization differentOrg = mock(Organization.class);
        when(differentOrg.getId()).thenReturn(differentOrgId);
        Invoice foreignInvoice = mock(Invoice.class);
        when(foreignInvoice.getId()).thenReturn(UUID.randomUUID());
        when(foreignInvoice.getOrganization()).thenReturn(differentOrg);

        ConfirmationLink foreignLink = new ConfirmationLink();
        foreignLink.setInvoice(foreignInvoice);

        PaymentConfirmation foreignConfirmation = new PaymentConfirmation();
        foreignConfirmation.setConfirmationLink(foreignLink);
        foreignConfirmation.setAmountClaimed(new BigDecimal("500.00"));

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(foreignConfirmation));

        // Must return 404 — never reveal that the resource exists in another org
        assertThrows(NotFoundException.class,
                () -> service.approveByBusiness(orgId, confirmationId, null));
    }

    @Test
    void approve_fails_whenConfirmationAlreadyApproved() {
        PaymentConfirmation alreadyApproved = buildPendingConfirmation(new BigDecimal("1000.00"));
        alreadyApproved.approve(null); // now APPROVED

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(alreadyApproved));

        assertThrows(ConflictException.class,
                () -> service.approveByBusiness(orgId, confirmationId, null));
        verify(paymentService, never()).recordGatewayPayment(any(), any(), any(), any(), any());
    }

    @Test
    void approve_fails_whenConfirmationAlreadyRejected() {
        PaymentConfirmation rejected = buildPendingConfirmation(new BigDecimal("1000.00"));
        rejected.reject(null); // now REJECTED

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(rejected));

        assertThrows(ConflictException.class,
                () -> service.approveByBusiness(orgId, confirmationId, null));
    }

    // ===================================================================
    // approveByBusiness — correct email routing by invoice state
    // ===================================================================

    @Test
    void approve_sendsFullConfirmedEmail_whenInvoiceBecomesFullyPaid() {
        PaymentConfirmation pendingConfirmation = buildPendingConfirmation(new BigDecimal("1000.00"));

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(pendingConfirmation));
        when(confirmationRepository.save(pendingConfirmation)).thenReturn(pendingConfirmation);

        Invoice paidInvoice = mock(Invoice.class);
        when(paidInvoice.isPaid()).thenReturn(true);
        when(invoiceService.getInvoiceById(invoiceId)).thenReturn(paidInvoice);

        ReviewConfirmationRequest request = new ReviewConfirmationRequest(); // notifyCustomer=true

        service.approveByBusiness(orgId, confirmationId, request);

        verify(businessNotificationService).notifyCustomerPaymentConfirmedFull(paidInvoice, pendingConfirmation);
        verify(businessNotificationService, never()).notifyCustomerPartialPaymentApproved(any(), any());
    }

    @Test
    void approve_sendsPartialApprovedEmail_whenInvoiceRemainsPartiallyPaid() {
        PaymentConfirmation pendingConfirmation = buildPendingConfirmation(new BigDecimal("300.00"));

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(pendingConfirmation));
        when(confirmationRepository.save(pendingConfirmation)).thenReturn(pendingConfirmation);

        Invoice partialInvoice = mock(Invoice.class);
        when(partialInvoice.isPaid()).thenReturn(false);
        when(invoiceService.getInvoiceById(invoiceId)).thenReturn(partialInvoice);

        ReviewConfirmationRequest request = new ReviewConfirmationRequest(); // notifyCustomer=true

        service.approveByBusiness(orgId, confirmationId, request);

        verify(businessNotificationService).notifyCustomerPartialPaymentApproved(partialInvoice, pendingConfirmation);
        verify(businessNotificationService, never()).notifyCustomerPaymentConfirmedFull(any(), any());
    }

    @Test
    void approve_skipsNotification_whenNotifyCustomerIsFalse() {
        PaymentConfirmation pendingConfirmation = buildPendingConfirmation(new BigDecimal("1000.00"));

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(pendingConfirmation));
        when(confirmationRepository.save(pendingConfirmation)).thenReturn(pendingConfirmation);
        when(invoiceService.getInvoiceById(invoiceId)).thenReturn(mock(Invoice.class));

        ReviewConfirmationRequest request = new ReviewConfirmationRequest();
        request.setNotifyCustomer(false);

        service.approveByBusiness(orgId, confirmationId, request);

        verify(businessNotificationService, never()).notifyCustomerPaymentConfirmedFull(any(), any());
        verify(businessNotificationService, never()).notifyCustomerPartialPaymentApproved(any(), any());
    }

    // ===================================================================
    // requestRemainingByBusiness — happy path
    // ===================================================================

    @Test
    void requestRemaining_recordsPartialPayment_andSendsInstallmentEmail() {
        // Invoice remaining = 1000, customer claims 400 → genuinely partial
        when(invoice.getRemainingAmount()).thenReturn(new BigDecimal("1000.00"));
        PaymentConfirmation pendingConfirmation = buildPendingConfirmation(new BigDecimal("400.00"));

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(pendingConfirmation));
        when(confirmationRepository.save(pendingConfirmation)).thenReturn(pendingConfirmation);

        Invoice updatedInvoice = mock(Invoice.class);
        when(updatedInvoice.getRemainingAmount()).thenReturn(new BigDecimal("600.00"));
        when(invoiceService.getInvoiceById(invoiceId)).thenReturn(updatedInvoice);

        PaymentConfirmation result = service.requestRemainingByBusiness(orgId, confirmationId, null);

        assertEquals(PaymentConfirmationStatus.APPROVED, result.getStatus());

        // Payment must be recorded
        verify(paymentService).recordGatewayPayment(
                eq(invoiceId), eq(new BigDecimal("400.00")),
                eq(PaymentMode.BANK_TRANSFER), any(), any());

        // Installment request email must be sent
        verify(businessNotificationService).notifyInstallmentRequest(updatedInvoice, pendingConfirmation);
    }

    @Test
    void requestRemaining_doesNotCloseLink_becauseInvoiceIsNotFullyPaid() {
        when(invoice.getRemainingAmount()).thenReturn(new BigDecimal("1000.00"));
        PaymentConfirmation pendingConfirmation = buildPendingConfirmation(new BigDecimal("400.00"));

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(pendingConfirmation));
        when(confirmationRepository.save(pendingConfirmation)).thenReturn(pendingConfirmation);
        when(invoiceService.getInvoiceById(invoiceId)).thenReturn(mock(Invoice.class));

        service.requestRemainingByBusiness(orgId, confirmationId, null);

        // Link must not be closed — balance still outstanding
        verify(confirmationLinkService, never()).closeForInvoice(any());
    }

    @Test
    void requestRemaining_skipsEmail_whenNotifyCustomerIsFalse() {
        when(invoice.getRemainingAmount()).thenReturn(new BigDecimal("1000.00"));
        PaymentConfirmation pendingConfirmation = buildPendingConfirmation(new BigDecimal("400.00"));

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(pendingConfirmation));
        when(confirmationRepository.save(pendingConfirmation)).thenReturn(pendingConfirmation);
        when(invoiceService.getInvoiceById(invoiceId)).thenReturn(mock(Invoice.class));

        ReviewConfirmationRequest request = new ReviewConfirmationRequest();
        request.setNotifyCustomer(false);

        service.requestRemainingByBusiness(orgId, confirmationId, request);

        verify(businessNotificationService, never()).notifyInstallmentRequest(any(), any());
    }

    // ===================================================================
    // requestRemainingByBusiness — guard: not a partial claim
    // ===================================================================

    @Test
    void requestRemaining_fails_whenClaimCoversFullRemainingBalance() {
        // remaining = 1000, claimed = 1000 → not a partial claim
        when(invoice.getRemainingAmount()).thenReturn(new BigDecimal("1000.00"));
        PaymentConfirmation pendingConfirmation = buildPendingConfirmation(new BigDecimal("1000.00"));

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(pendingConfirmation));

        ConflictException ex = assertThrows(ConflictException.class,
                () -> service.requestRemainingByBusiness(orgId, confirmationId, null));
        assertTrue(ex.getMessage().contains("covers the full remaining balance"));
        verify(paymentService, never()).recordGatewayPayment(any(), any(), any(), any(), any());
    }

    @Test
    void requestRemaining_fails_whenClaimExceedsRemainingBalance() {
        // This shouldn't happen (submitByCustomer guards it), but belt-and-suspenders
        when(invoice.getRemainingAmount()).thenReturn(new BigDecimal("500.00"));
        PaymentConfirmation pendingConfirmation = buildPendingConfirmation(new BigDecimal("600.00"));

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(pendingConfirmation));

        assertThrows(ConflictException.class,
                () -> service.requestRemainingByBusiness(orgId, confirmationId, null));
        verify(paymentService, never()).recordGatewayPayment(any(), any(), any(), any(), any());
    }

    // ===================================================================
    // requestRemainingByBusiness — auth/state guards
    // ===================================================================

    @Test
    void requestRemaining_fails_whenConfirmationNotFound() {
        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> service.requestRemainingByBusiness(orgId, confirmationId, null));
    }

    @Test
    void requestRemaining_fails_whenConfirmationAlreadyApproved() {
        when(invoice.getRemainingAmount()).thenReturn(new BigDecimal("1000.00"));
        PaymentConfirmation alreadyApproved = buildPendingConfirmation(new BigDecimal("400.00"));
        alreadyApproved.approve(null);

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(alreadyApproved));

        assertThrows(ConflictException.class,
                () -> service.requestRemainingByBusiness(orgId, confirmationId, null));
        verify(paymentService, never()).recordGatewayPayment(any(), any(), any(), any(), any());
    }

    @Test
    void requestRemaining_fails_whenConfirmationBelongsToDifferentOrg() {
        UUID differentOrgId = UUID.randomUUID();
        Organization differentOrg = mock(Organization.class);
        when(differentOrg.getId()).thenReturn(differentOrgId);
        Invoice foreignInvoice = mock(Invoice.class);
        when(foreignInvoice.getOrganization()).thenReturn(differentOrg);
        ConfirmationLink foreignLink = new ConfirmationLink();
        foreignLink.setInvoice(foreignInvoice);
        PaymentConfirmation foreignConfirmation = new PaymentConfirmation();
        foreignConfirmation.setConfirmationLink(foreignLink);
        foreignConfirmation.setAmountClaimed(new BigDecimal("400.00"));

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(foreignConfirmation));

        assertThrows(NotFoundException.class,
                () -> service.requestRemainingByBusiness(orgId, confirmationId, null));
    }

    // ===================================================================
    // rejectByBusiness — happy path & failures
    // ===================================================================

    @Test
    void reject_setsStatusToRejected_andDoesNotRecordPayment() {
        PaymentConfirmation pendingConfirmation = buildPendingConfirmation(new BigDecimal("1000.00"));

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(pendingConfirmation));
        when(confirmationRepository.save(pendingConfirmation)).thenReturn(pendingConfirmation);

        ReviewConfirmationRequest reviewRequest = new ReviewConfirmationRequest();
        reviewRequest.setNote("Amount does not match our records");

        PaymentConfirmation result = service.rejectByBusiness(orgId, confirmationId, reviewRequest);

        assertEquals(PaymentConfirmationStatus.REJECTED, result.getStatus());
        assertEquals("Amount does not match our records", result.getBusinessNote());
        // Payment must NOT be recorded on rejection
        verify(paymentService, never()).recordGatewayPayment(any(), any(), any(), any(), any());
        // Link must NOT be closed — customer can resubmit
        verify(confirmationLinkService, never()).closeForInvoice(any());
    }

    @Test
    void reject_fails_whenConfirmationNotPending() {
        PaymentConfirmation alreadyApproved = buildPendingConfirmation(new BigDecimal("1000.00"));
        alreadyApproved.approve(null);

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(alreadyApproved));

        assertThrows(ConflictException.class,
                () -> service.rejectByBusiness(orgId, confirmationId, null));
    }

    @Test
    void reject_fails_whenConfirmationBelongsToDifferentOrg() {
        UUID differentOrgId = UUID.randomUUID();
        Organization differentOrg = mock(Organization.class);
        when(differentOrg.getId()).thenReturn(differentOrgId);
        Invoice foreignInvoice = mock(Invoice.class);
        when(foreignInvoice.getOrganization()).thenReturn(differentOrg);
        ConfirmationLink foreignLink = new ConfirmationLink();
        foreignLink.setInvoice(foreignInvoice);
        PaymentConfirmation foreignConfirmation = new PaymentConfirmation();
        foreignConfirmation.setConfirmationLink(foreignLink);
        foreignConfirmation.setAmountClaimed(new BigDecimal("500.00"));

        when(organizationService.getById(orgId)).thenReturn(organization);
        when(confirmationRepository.findById(confirmationId)).thenReturn(Optional.of(foreignConfirmation));

        assertThrows(NotFoundException.class,
                () -> service.rejectByBusiness(orgId, confirmationId, null));
    }

    // ===================================================================
    // PaymentConfirmation domain state machine
    // ===================================================================

    @Test
    void domainEntity_cannotApprove_alreadyApprovedConfirmation() {
        PaymentConfirmation confirmation = new PaymentConfirmation();
        confirmation.setAmountClaimed(BigDecimal.TEN);
        confirmation.approve(null);

        assertThrows(IllegalStateException.class, () -> confirmation.approve(null));
    }

    @Test
    void domainEntity_cannotReject_alreadyRejectedConfirmation() {
        PaymentConfirmation confirmation = new PaymentConfirmation();
        confirmation.setAmountClaimed(BigDecimal.TEN);
        confirmation.reject(null);

        assertThrows(IllegalStateException.class, () -> confirmation.reject(null));
    }

    @Test
    void domainEntity_cannotApprove_afterRejection() {
        PaymentConfirmation confirmation = new PaymentConfirmation();
        confirmation.setAmountClaimed(BigDecimal.TEN);
        confirmation.reject("wrong amount");

        assertThrows(IllegalStateException.class, () -> confirmation.approve(null));
    }

    // ===================================================================
    // ConfirmationLink domain state machine
    // ===================================================================

    @Test
    void confirmationLink_isOpen_byDefault() {
        ConfirmationLink link = new ConfirmationLink();
        assertTrue(link.isOpen());
    }

    @Test
    void confirmationLink_close_isIdempotent() {
        ConfirmationLink link = new ConfirmationLink();
        link.close();
        link.close(); // second close must not throw
        assertFalse(link.isOpen());
    }

    // ===================================================================
    // Helpers
    // ===================================================================

    private PaymentConfirmation buildPendingConfirmation(BigDecimal amount) {
        ConfirmationLink link = new ConfirmationLink();
        link.setInvoice(invoice);

        PaymentConfirmation confirmation = new PaymentConfirmation();
        confirmation.setConfirmationLink(link);
        confirmation.setAmountClaimed(amount);
        // status defaults to PENDING_APPROVAL
        return confirmation;
    }
}
