package com.paidpeace.application.invoice;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paidpeace.domain.invoice.Invoice;
import com.paidpeace.domain.invoice.LifeCycleStatus;
import com.paidpeace.domain.invoice.TimeStatus;
import com.paidpeace.domain.organization.Organization;
import com.paidpeace.domain.organization.OrganizationStatus;
import com.paidpeace.application.invoice.InvoiceService;
import com.paidpeace.application.organization.OrganizationService;

@Service
public class InvoiceStatusSyncService {

    private static final List<OrganizationStatus> ELIGIBLE_ORGANIZATION_STATUSES = List.of(
            OrganizationStatus.ACTIVE,
            OrganizationStatus.TRIAL
    );

    private static final List<LifeCycleStatus> ELIGIBLE_INVOICE_STATUSES = List.of(
            LifeCycleStatus.ISSUED,
            LifeCycleStatus.PARTIALLY_PAID
    );

    private static final Logger log = LoggerFactory.getLogger(InvoiceStatusSyncService.class);

    private final OrganizationService organizationService;
    private final InvoiceService invoiceService;

    public InvoiceStatusSyncService(
            OrganizationService organizationService,
            InvoiceService invoiceService
    ) {
        this.organizationService = organizationService;
        this.invoiceService = invoiceService;
    }

    @Transactional
    public void syncIssuedInvoiceTimeStatuses() {
        int updatedCount = 0;

        List<Organization> organizations = organizationService.getEligibleOrganizationsForReminders(
                ELIGIBLE_ORGANIZATION_STATUSES
        );
        for (Organization organization : organizations) {
            LocalDate organizationToday = LocalDate.now(organization.getTimezone());
            List<Invoice> eligibleInvoices = invoiceService.getInvoicesWithDueDate(
                    organization.getId(),
                    ELIGIBLE_INVOICE_STATUSES
            );

            List<Invoice> changedInvoices = new ArrayList<>();
            for (Invoice invoice : eligibleInvoices) {
                TimeStatus previousStatus = invoice.getTimeStatus();
                invoice.refreshTimeStatus(organizationToday);
                if (previousStatus != invoice.getTimeStatus()) {
                    changedInvoices.add(invoice);
                }
            }

            if (!changedInvoices.isEmpty()) {
                invoiceService.saveAll(changedInvoices);
                updatedCount += changedInvoices.size();
            }
        }

        log.info("Invoice time status sync completed. updated={}", updatedCount);
    }
}
