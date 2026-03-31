package com.flowcollect.application.reminder;

import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.followup.FollowUpChannel;

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
}
