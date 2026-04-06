package com.flowcollect.application.confirmation;

import com.flowcollect.domain.confirmation.ConfirmationLink;
import com.flowcollect.domain.confirmation.ConfirmationLinkStatus;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.exception.http.NotFoundException;
import com.flowcollect.infrastructure.persistence.confirmation.ConfirmationLinkJpaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConfirmationLinkServiceTest {

    @Mock private ConfirmationLinkJpaRepository confirmationLinkRepository;
    @Mock private Invoice invoice;

    private ConfirmationLinkService confirmationLinkService;

    private final UUID invoiceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        confirmationLinkService = new ConfirmationLinkService(confirmationLinkRepository);
        ReflectionTestUtils.setField(confirmationLinkService, "appFrontendUrl", "https://app.flowcollect.io");
        when(invoice.getId()).thenReturn(invoiceId);
    }

    // -----------------------------------------------------------------------
    // getOrCreateForInvoice
    // -----------------------------------------------------------------------

    @Test
    void getOrCreate_createsNewLink_whenNoneExists() {
        when(confirmationLinkRepository.findByInvoiceId(invoiceId)).thenReturn(Optional.empty());
        when(confirmationLinkRepository.save(any(ConfirmationLink.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ConfirmationLink result = confirmationLinkService.getOrCreateForInvoice(invoice);

        assertNotNull(result);
        assertNotNull(result.getToken());
        assertEquals(32, result.getToken().length()); // UUID without dashes = 32 hex chars
        assertTrue(result.getPublicUrl().startsWith("https://app.flowcollect.io/confirm/"));
        assertTrue(result.getPublicUrl().endsWith(result.getToken()));
        verify(confirmationLinkRepository).save(any(ConfirmationLink.class));
    }

    @Test
    void getOrCreate_returnsExistingLink_whenAlreadyCreated() {
        ConfirmationLink existing = new ConfirmationLink();
        existing.setToken("abc123def456abc123def456abc12345");
        existing.setPublicUrl("https://app.flowcollect.io/confirm/abc123def456abc123def456abc12345");
        when(confirmationLinkRepository.findByInvoiceId(invoiceId)).thenReturn(Optional.of(existing));

        ConfirmationLink result = confirmationLinkService.getOrCreateForInvoice(invoice);

        assertSame(existing, result);
        // Must NOT save a new link — the existing one is returned as-is
        verify(confirmationLinkRepository, never()).save(any());
    }

    @Test
    void getOrCreate_generatesDifferentTokens_forDifferentInvoices() {
        Invoice invoice2 = mock(Invoice.class);
        when(invoice2.getId()).thenReturn(UUID.randomUUID());

        when(confirmationLinkRepository.findByInvoiceId(any())).thenReturn(Optional.empty());
        when(confirmationLinkRepository.save(any(ConfirmationLink.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ConfirmationLink link1 = confirmationLinkService.getOrCreateForInvoice(invoice);
        ConfirmationLink link2 = confirmationLinkService.getOrCreateForInvoice(invoice2);

        assertNotEquals(link1.getToken(), link2.getToken());
    }

    // -----------------------------------------------------------------------
    // getByToken
    // -----------------------------------------------------------------------

    @Test
    void getByToken_returnsLink_whenTokenExists() {
        ConfirmationLink link = new ConfirmationLink();
        link.setToken("validtoken12345678901234567890ab");
        when(confirmationLinkRepository.findByToken("validtoken12345678901234567890ab"))
                .thenReturn(Optional.of(link));

        ConfirmationLink result = confirmationLinkService.getByToken("validtoken12345678901234567890ab");

        assertSame(link, result);
    }

    @Test
    void getByToken_throwsNotFound_whenTokenMissing() {
        when(confirmationLinkRepository.findByToken("nonexistent")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> confirmationLinkService.getByToken("nonexistent"));
    }

    @Test
    void getByToken_throwsNotFound_whenTokenIsNull() {
        assertThrows(NotFoundException.class,
                () -> confirmationLinkService.getByToken(null));
    }

    @Test
    void getByToken_throwsNotFound_whenTokenIsBlank() {
        assertThrows(NotFoundException.class,
                () -> confirmationLinkService.getByToken("   "));
    }

    // -----------------------------------------------------------------------
    // closeForInvoice
    // -----------------------------------------------------------------------

    @Test
    void closeForInvoice_closesOpenLink() {
        ConfirmationLink link = new ConfirmationLink();
        // Starts OPEN by default
        assertTrue(link.isOpen());

        when(confirmationLinkRepository.findByInvoiceId(invoiceId)).thenReturn(Optional.of(link));
        when(confirmationLinkRepository.save(link)).thenReturn(link);

        confirmationLinkService.closeForInvoice(invoiceId);

        assertFalse(link.isOpen());
        assertEquals(ConfirmationLinkStatus.CLOSED, link.getStatus());
        verify(confirmationLinkRepository).save(link);
    }

    @Test
    void closeForInvoice_isIdempotent_whenLinkAlreadyClosed() {
        ConfirmationLink link = new ConfirmationLink();
        link.close(); // already closed

        when(confirmationLinkRepository.findByInvoiceId(invoiceId)).thenReturn(Optional.of(link));

        confirmationLinkService.closeForInvoice(invoiceId);

        // save must NOT be called again — no-op
        verify(confirmationLinkRepository, never()).save(any());
    }

    @Test
    void closeForInvoice_isNoOp_whenNoLinkExists() {
        when(confirmationLinkRepository.findByInvoiceId(invoiceId)).thenReturn(Optional.empty());

        // Must not throw
        assertDoesNotThrow(() -> confirmationLinkService.closeForInvoice(invoiceId));
        verify(confirmationLinkRepository, never()).save(any());
    }
}
