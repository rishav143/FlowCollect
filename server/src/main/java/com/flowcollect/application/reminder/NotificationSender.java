package com.flowcollect.application.reminder;

import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.followup.FollowUpChannel;

import java.util.List;

/**
 * Abstraction for channel-specific reminder delivery.
 * ReminderEngine depends on this interface instead of concrete providers.
 */
public interface NotificationSender {

    FollowUpChannel channel();

    /** Sends the notification and returns an optional external message ID (e.g. Resend email ID), or null. */
    String send(Customer customer, String subject, String body);

    default String send(Customer customer, String subject, String body, boolean attachPdf, Invoice invoice) {
        return send(customer, subject, body);
    }

    /**
     * Sends a single consolidated notification for multiple invoices.
     * The body should already contain the full list of invoice details rendered by the caller.
     * When attachPdf is true, implementations that support attachments (email, WhatsApp) will
     * generate and attach one PDF per invoice.
     *
     * <p>Default: falls back to the single-invoice variant with the first invoice (or no PDF).
     * Channel implementations that support multi-attachment should override this method.
     */
    default String sendConsolidated(Customer customer, String subject, String body,
                                    List<Invoice> invoices) {
        Invoice first = (invoices != null && !invoices.isEmpty()) ? invoices.get(0) : null;
        return send(customer, subject, body, false, first);
    }
}
