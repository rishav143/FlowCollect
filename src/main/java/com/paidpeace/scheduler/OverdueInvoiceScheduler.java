package com.paidpeace.scheduler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.paidpeace.application.invoice.InvoiceStatusSyncService;

@Component
public class OverdueInvoiceScheduler {

    private final InvoiceStatusSyncService invoiceStatusSyncService;
    private final boolean schedulerEnabled;

    public OverdueInvoiceScheduler(
            InvoiceStatusSyncService invoiceStatusSyncService,
            @Value("${scheduler.enabled:true}") boolean schedulerEnabled
    ) {
        this.invoiceStatusSyncService = invoiceStatusSyncService;
        this.schedulerEnabled = schedulerEnabled;
    }

    @Scheduled(cron = "${scheduler.invoice-status.cron:0 0 * * * *}")
    public void runInvoiceStatusSyncJob() {
        if (!schedulerEnabled) {
            return;
        }
        invoiceStatusSyncService.syncIssuedInvoiceTimeStatuses();
    }
}
