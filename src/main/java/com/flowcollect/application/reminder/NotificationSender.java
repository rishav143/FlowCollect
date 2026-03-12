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

    void send(Customer customer, String subject, String body);

    default void send(Customer customer, String subject, String body, boolean attachPdf, Invoice invoice) {
        send(customer, subject, body);
    }
}
