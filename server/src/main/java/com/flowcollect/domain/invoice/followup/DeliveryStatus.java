package com.flowcollect.domain.invoice.followup;

/**
 * Async delivery confirmation status for SMS and WhatsApp follow-ups.
 * Set via provider webhooks (Twilio for SMS, Meta for WhatsApp) after
 * the message has been submitted (FollowUpStatus = SENT).
 *
 * EMAIL delivery is tracked via Resend's own webhook; this enum is not
 * used for the EMAIL channel.
 */
public enum DeliveryStatus {
    /** Provider confirmed the message was delivered to the recipient's device. */
    DELIVERED,
    /** Provider reported delivery failure (undelivered, failed, or error). */
    UNDELIVERED
}
