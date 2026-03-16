package com.flowcollect.scheduler;

import com.flowcollect.application.paymentlink.PaymentLinkService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Marks ACTIVE and PARTIALLY_PAID payment links as EXPIRED once their expiry
 * date has passed. Runs daily at midnight by default.
 */
@Component
public class PaymentLinkExpiryScheduler {

    private final PaymentLinkService paymentLinkService;
    private final boolean schedulerEnabled;

    public PaymentLinkExpiryScheduler(
            PaymentLinkService paymentLinkService,
            
            @Value("${scheduler.enabled:true}") boolean schedulerEnabled
    ) {
        this.paymentLinkService = paymentLinkService;
        this.schedulerEnabled = schedulerEnabled;
    }

    @Scheduled(cron = "${scheduler.payment-link-expiry.cron:0 0 0 * * *}")
    public void expireOverduePaymentLinks() {
        if (!schedulerEnabled) {
            return;
        }
        paymentLinkService.expireOverdueLinks();
    }
}
