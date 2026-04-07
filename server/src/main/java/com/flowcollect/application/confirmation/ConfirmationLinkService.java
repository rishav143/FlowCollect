package com.flowcollect.application.confirmation;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowcollect.domain.confirmation.ConfirmationLink;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.exception.http.NotFoundException;
import com.flowcollect.infrastructure.persistence.confirmation.ConfirmationLinkJpaRepository;

/**
 * Manages the lifecycle of {@link ConfirmationLink} entities.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Creating links lazily (on first follow-up dispatch) to avoid orphaned records.</li>
 *   <li>Returning the existing link idempotently — safe to call on every dispatch.</li>
 *   <li>Looking up links by public token for customer-facing endpoints.</li>
 *   <li>Closing links when the associated invoice becomes fully paid or is cancelled.</li>
 * </ul>
 */
@Service
public class ConfirmationLinkService {

    private final ConfirmationLinkJpaRepository confirmationLinkRepository;

    @Value("${app.frontend-url}")
    private String appFrontendUrl;

    public ConfirmationLinkService(ConfirmationLinkJpaRepository confirmationLinkRepository) {
        this.confirmationLinkRepository = confirmationLinkRepository;
    }

    /**
     * Returns the existing {@link ConfirmationLink} for the invoice, or creates one if none exists.
     *
     * <p>Idempotent: safe to call multiple times for the same invoice. The same link (and URL)
     * is returned on every call, ensuring customers always receive a consistent link across reminders.
     *
     * @param invoice the issued invoice that needs a confirmation link
     * @return the existing or newly created confirmation link
     */
    @Transactional
    public ConfirmationLink getOrCreateForInvoice(Invoice invoice) {
        return confirmationLinkRepository.findByInvoiceId(invoice.getId())
                .orElseGet(() -> createForInvoice(invoice));
    }

    /**
     * Looks up a confirmation link by its public token.
     *
     * @param token the 32-character hex token embedded in the public URL
     * @return the matching confirmation link
     * @throws NotFoundException if the token does not match any known link
     */
    @Transactional(readOnly = true)
    public ConfirmationLink getByToken(String token) {
        if (token == null || token.isBlank()) {
            throw new NotFoundException("Confirmation link not found");
        }
        return confirmationLinkRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Confirmation link not found"));
    }

    /**
     * Closes the confirmation link for the given invoice, if one exists and is still open.
     *
     * <p>Called by {@link PaymentConfirmationService} after an approval causes the invoice
     * to become fully paid, and may also be called when an invoice is cancelled.
     * Idempotent: closing an already-closed link is a no-op.
     *
     * @param invoiceId the ID of the invoice whose link should be closed
     */
    @Transactional
    public void closeForInvoice(UUID invoiceId) {
        confirmationLinkRepository.findByInvoiceId(invoiceId).ifPresent(link -> {
            if (link.isOpen()) {
                link.close();
                confirmationLinkRepository.save(link);
            }
        });
    }

    /**
     * Reopens the confirmation link for the given invoice if it exists and is currently closed.
     *
     * <p>Called when a recorded payment is deleted or reduced and the invoice reverts to an
     * unpaid state (ISSUED or PARTIALLY_PAID). This allows the customer to resubmit via the
     * same link that was already sent to them.
     * Idempotent: no-op if the link is already open or does not exist.
     *
     * @param invoiceId the ID of the invoice whose link should be reopened
     */
    @Transactional
    public void reopenForInvoice(UUID invoiceId) {
        confirmationLinkRepository.findByInvoiceId(invoiceId).ifPresent(link -> {
            if (!link.isOpen()) {
                link.reopen();
                confirmationLinkRepository.save(link);
            }
        });
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private ConfirmationLink createForInvoice(Invoice invoice) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String publicUrl = appFrontendUrl + "/confirm/" + token;

        ConfirmationLink link = new ConfirmationLink();
        link.setInvoice(invoice);
        link.setToken(token);
        link.setPublicUrl(publicUrl);
        return confirmationLinkRepository.save(link);
    }
}
