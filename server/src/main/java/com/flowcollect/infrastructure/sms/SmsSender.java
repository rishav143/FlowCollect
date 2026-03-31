package com.flowcollect.infrastructure.sms;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.flowcollect.application.reminder.NotificationSender;
import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.invoice.followup.FollowUpChannel;
import com.flowcollect.exception.http.InternalException;
import com.flowcollect.infrastructure.config.TwilioProperties;

@Component
public class SmsSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(SmsSender.class);

    private final TwilioProperties properties;

    public SmsSender(TwilioProperties properties) {
        this.properties = properties;
    }

    @Override
    public FollowUpChannel channel() {
        return FollowUpChannel.SMS;
    }

    @Override
    public String send(Customer customer, String subject, String body) {
        ensureEnabled();

        String recipient = requireConfigured(customer.getPhone(), "customer phone");
        Twilio.init(requireConfigured(properties.getAccountSid(), "twilio.account-sid"),
                requireConfigured(properties.getAuthToken(), "twilio.auth-token"));

        String smsBody = subject != null && !subject.isBlank()
                ? subject + System.lineSeparator() + System.lineSeparator() + (body == null ? "" : body)
                : (body == null ? "" : body);

        try {
            Message.creator(
                    new PhoneNumber(recipient.trim()),
                    new PhoneNumber(requireConfigured(properties.getSmsFrom(), "twilio.sms-from")),
                    smsBody
            ).create();
            log.info("Sent SMS reminder to {}", recipient);
        } catch (Exception ex) {
            throw new InternalException("Failed to send SMS reminder: " + ex.getMessage());
        }
        return null;
    }

    private void ensureEnabled() {
        if (!properties.isEnabled()) {
            throw new InternalException("Twilio delivery is disabled");
        }
    }

    private String requireConfigured(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new InternalException("Missing required Twilio configuration: " + propertyName);
        }
        return value.trim();
    }
}
