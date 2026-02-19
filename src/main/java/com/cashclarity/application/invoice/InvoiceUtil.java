package com.cashclarity.application.invoice;

import com.cashclarity.domain.invoice.Invoice;
import com.cashclarity.exception.invoice.InvalidInvoiceFieldException;
import com.cashclarity.exception.invoice.InvoiceNotFoundException;
import com.cashclarity.exception.organization.InvalidOrganizationFieldException;
import com.cashclarity.exception.organization.OrganizationNotFoundException;
import com.cashclarity.infrastructure.persistence.invoice.InvoiceJpaRepository;

import java.util.List;
import java.util.UUID;
import com.cashclarity.api.v1.invoice.dto.InvoiceItemRequest;


public class InvoiceUtil {
    public static void addItems(Invoice invoice, List<InvoiceItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (InvoiceItemRequest item : items) {
            invoice.addItem(item.getDescription(), item.getQuantity(), item.getUnitPrice());
        }
    }

    public static Invoice getInvoiceOrThrow(UUID invoiceId, InvoiceJpaRepository invoiceRepository) {
        if (invoiceId == null) {
            throw new InvalidInvoiceFieldException("invoiceId must not be null");
        }
        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new InvoiceNotFoundException("invoice not found with id " + invoiceId));
        if (invoice.getOrganization().isDeleted()) {
            throw new OrganizationNotFoundException("organization is archived with id " + invoice.getOrganization().getId());
        }
        return invoice;
    }

    public static Invoice validateInvoiceWithOrganization(UUID invoiceId, UUID organizationId, InvoiceJpaRepository invoiceRepository) {
        if (invoiceId == null) {
            throw new InvalidInvoiceFieldException("invoiceId must not be null");
        }
        if (organizationId == null) {
            throw new InvalidOrganizationFieldException("organizationId must not be null");
        }
        Invoice invoice = getInvoiceOrThrow(invoiceId, invoiceRepository);
        if (invoice.getOrganization().getId() != organizationId) {
            throw new InvalidInvoiceFieldException("invoice is not associated with organization with id " + organizationId);
        }
        return invoice;
    }
}
