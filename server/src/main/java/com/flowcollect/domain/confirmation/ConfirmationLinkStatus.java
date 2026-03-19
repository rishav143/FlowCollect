package com.flowcollect.domain.confirmation;

/**
 * Lifecycle status of a {@link ConfirmationLink}.
 *
 * <ul>
 *   <li>{@link #OPEN} — the link is accepting customer payment submissions.</li>
 *   <li>{@link #CLOSED} — the invoice has been fully paid (or cancelled); no further
 *       submissions are accepted. The link URL still resolves but returns an informational page.</li>
 * </ul>
 */
public enum ConfirmationLinkStatus {
    OPEN,
    CLOSED
}