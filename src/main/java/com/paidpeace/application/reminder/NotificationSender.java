package com.paidpeace.application.reminder;

import com.paidpeace.domain.customer.Customer;
import com.paidpeace.domain.invoice.Invoice;
import com.paidpeace.domain.invoice.followup.FollowUpChannel;

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
