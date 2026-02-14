package com.cashclarity.application.invoice;

import com.cashclarity.application.util;
import com.cashclarity.domain.invoice.Invoice;
import com.cashclarity.exception.invoice.InvoiceNotFoundException;
import com.cashclarity.exception.invoice.InvalidInvoiceFieldException;
import com.cashclarity.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.cashclarity.infrastructure.persistence.organization.OrganizationJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteInvoiceService {

    private final InvoiceJpaRepository invoiceRepository;
    private final OrganizationJpaRepository organizationRepository;

    public DeleteInvoiceService(InvoiceJpaRepository invoiceRepository, OrganizationJpaRepository organizationRepository) {
        this.invoiceRepository = invoiceRepository;
        this.organizationRepository = organizationRepository;
    }

    /**
     * Hard delete an invoice if and only if it is in DRAFT state and belongs to the organization.
     * Throws clear exceptions to propagate to controller for debugging.
     */
    @Transactional
    public void deleteInvoice(Long organizationId, Long invoiceId) {
        if (invoiceId == null || invoiceId <= 0) {
            throw new InvalidInvoiceFieldException("invoiceId", "must be a positive number");
        }

        util.validateOrganizationId(organizationId);
        util.getOrganizationOrThrow(organizationId, organizationRepository);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        if (!invoice.getOrganization().getId().equals(organizationId)) {
            throw new InvalidInvoiceFieldException("organizationId", "invoice does not belong to organization " + organizationId);
        }

        if (!invoice.isDraft()) {
            throw new InvalidInvoiceFieldException("lifeCycleStatus", "only draft invoices may be deleted");
        }

        // Hard delete - JPA will cascade delete items due to orphanRemoval/cascade settings
        invoiceRepository.delete(invoice);
    }
}

