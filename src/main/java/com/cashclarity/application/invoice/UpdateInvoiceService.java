package com.cashclarity.application.invoice;

import com.cashclarity.api.v1.invoice.dto.InvoiceUpdateRequest;
import com.cashclarity.application.util;
import com.cashclarity.domain.customer.Customer;
import com.cashclarity.domain.invoice.Invoice;
import com.cashclarity.domain.invoice.InvoiceItem;
import com.cashclarity.domain.user.User;
import com.cashclarity.exception.Customer.CustomerNotFoundException;
import com.cashclarity.exception.invoice.InvoiceAlreadyExistsException;
import com.cashclarity.exception.invoice.InvoiceNotFoundException;
import com.cashclarity.exception.invoice.InvalidInvoiceFieldException;
import com.cashclarity.exception.organization.InvalidOrganizationIdException;
import com.cashclarity.exception.user.UserNotFoundException;
import com.cashclarity.infrastructure.persistence.customer.CustomerJpaRepository;
import com.cashclarity.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.cashclarity.infrastructure.persistence.organization.OrganizationJpaRepository;
import com.cashclarity.infrastructure.persistence.user.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UpdateInvoiceService {

    private final InvoiceJpaRepository invoiceRepository;
    private final OrganizationJpaRepository organizationRepository;
    private final UserJpaRepository userRepository;
    private final CustomerJpaRepository customerRepository;

    public UpdateInvoiceService(
            InvoiceJpaRepository invoiceRepository,
            OrganizationJpaRepository organizationRepository,
            UserJpaRepository userRepository,
            CustomerJpaRepository customerRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Updates mutable fields of a draft invoice.
     * Throws exceptions with clear messages for easier debugging.
     */
    @Transactional
    public Invoice updateInvoice(Long organizationId, Long invoiceId, InvoiceUpdateRequest request) {

        if (request == null) {
            throw new InvalidInvoiceFieldException("request", "must not be null");
        }

        util.validateOrganizationId(organizationId);

        // ensure organization exists and not archived
        util.getOrganizationOrThrow(organizationId, organizationRepository);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        if (!invoice.getOrganization().getId().equals(organizationId)) {
            throw new InvalidOrganizationIdException(invoice.getOrganization().getId(), organizationId);
        }

        if (!invoice.isDraft()) {
            throw new InvalidInvoiceFieldException("lifeCycleStatus", "only draft invoices may be updated");
        }

        // invoice number update - ensure uniqueness within organization when changed
        if (request.getInvoiceNumber() != null) {
            String normalized = request.getInvoiceNumber().trim();
            if (normalized.isBlank()) {
                throw new InvalidInvoiceFieldException("invoiceNumber", "must not be blank");
            }
            if (!normalized.equals(invoice.getInvoiceNumber())
                    && invoiceRepository.existsByInvoiceNumberAndOrganizationId(normalized, organizationId)) {
                throw new InvoiceAlreadyExistsException(normalized, organizationId);
            }
            invoice.setInvoiceNumber(normalized);
        }

        // tax percentage
        if (request.getTaxPercentage() != null) {
            invoice.setTaxPercentage(request.getTaxPercentage());
        }

        // createdBy user
        if (request.getCreatedByUserId() != null) {
            util.validateCreatedByUserId(request.getCreatedByUserId());
            User user = userRepository.findById(request.getCreatedByUserId())
                    .orElseThrow(() -> new UserNotFoundException(request.getCreatedByUserId()));
            invoice.setCreatedBy(user);
        }

        // customer
        if (request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                    .orElseThrow(() -> new CustomerNotFoundException(request.getCustomerId()));
            invoice.setCustomer(customer);
        }

        // due date
        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        }

        // items - replace existing items with provided list
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            // remove existing items safely
            List<InvoiceItem> existing = new ArrayList<>(invoice.getItems());
            for (InvoiceItem item : existing) {
                invoice.removeItem(item);
            }
            // add new items (util.addItems will validate)
            util.addItems(invoice, request.getItems());
        }

        // Save and return
        return invoiceRepository.save(invoice);
    }
}

