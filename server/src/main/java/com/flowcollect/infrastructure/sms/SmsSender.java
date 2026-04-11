package com.flowcollect.infrastructure.sms;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
    private final String appBaseUrl;

    public SmsSender(
            TwilioProperties properties,
            @Value("${app.base-url}") String appBaseUrl
    ) {
        this.properties = properties;
        this.appBaseUrl  = appBaseUrl;
    }

    @Override
    public FollowUpChannel channel() {
        return FollowUpChannel.SMS;
    }

    /**
     * Sends an SMS via Twilio and returns the Twilio message SID.
     * A statusCallback URL is registered so Twilio can POST delivery
     * status updates back to our webhook endpoint.
     */
    @Override
    public String send(Customer customer, String subject, String body) {
        ensureEnabled();

        String recipient = requireConfigured(customer.getPhone(), "customer phone");
        Twilio.init(requireConfigured(properties.getAccountSid(), "twilio.account-sid"),
                requireConfigured(properties.getAuthToken(), "twilio.auth-token"));

        String smsBody = subject != null && !subject.isBlank()
                ? subject + System.lineSeparator() + System.lineSeparator() + (body == null ? "" : body)
                : (body == null ? "" : body);

        String statusCallbackUrl = appBaseUrl + "/api/v1/webhooks/twilio/status";

        try {
            Message message = Message.creator(
                    new PhoneNumber(recipient.trim()),
                    new PhoneNumber(requireConfigured(properties.getSmsFrom(), "twilio.sms-from")),
                    smsBody
            ).setStatusCallback(statusCallbackUrl).create();

            log.info("Sent SMS reminder to {} — SID={}", recipient, message.getSid());
            return message.getSid();
        } catch (Exception ex) {
            throw new InternalException("Failed to send SMS reminder: " + ex.getMessage());
        }
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
