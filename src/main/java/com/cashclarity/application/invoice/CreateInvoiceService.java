package com.cashclarity.application.invoice;

import com.cashclarity.api.v1.invoice.dto.InvoiceRequest;
import com.cashclarity.application.util;
import com.cashclarity.domain.customer.Customer;
import com.cashclarity.domain.invoice.Invoice;
import com.cashclarity.domain.organization.Organization;
import com.cashclarity.domain.user.User;
import com.cashclarity.exception.invoice.InvoiceAlreadyExistsException;
import com.cashclarity.exception.Customer.CustomerNotFoundException;
import com.cashclarity.exception.invoice.InvalidInvoiceFieldException;
import com.cashclarity.exception.organization.InvalidOrganizationIdException;
import com.cashclarity.exception.user.UserNotFoundException;
import com.cashclarity.infrastructure.persistence.customer.CustomerJpaRepository;
import com.cashclarity.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.cashclarity.infrastructure.persistence.organization.OrganizationJpaRepository;
import com.cashclarity.infrastructure.persistence.user.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CreateInvoiceService {

    private final InvoiceJpaRepository invoiceRepository;
    private final OrganizationJpaRepository organizationRepository;
    private final UserJpaRepository userRepository;
    private final CustomerJpaRepository customerRepository;

    public CreateInvoiceService(
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
     * Creates a draft invoice.
     * <p>
     * Exceptions are intentionally propagated to the API layer so they can be
     * centrally handled (and debugged) by GlobalExceptionHandler.
     */
    @Transactional
    public Invoice createInvoice(Long organizationId, InvoiceRequest request) {

        // Validate request otherwise throw exception
        if (request == null) {
            throw new InvalidInvoiceFieldException("request", "must not be null");
        }

        if(request.getOrganizationId() != organizationId) {
            throw new InvalidOrganizationIdException(request.getOrganizationId(), organizationId);
        }

        util.validateOrganizationId(organizationId);

        Organization organization = util.getOrganizationOrThrow(organizationId, organizationRepository);

        String normalizedInvoiceNumber = request.getInvoiceNumber().trim();
        if (invoiceRepository.existsByInvoiceNumberAndOrganizationId(normalizedInvoiceNumber, request.getOrganizationId())) {
            throw new InvoiceAlreadyExistsException(normalizedInvoiceNumber, request.getOrganizationId());
        }

        // Create invoice
        Invoice invoice = Invoice.create(
                organization,
                normalizedInvoiceNumber
        );

        if (request.getTaxPercentage() != null) {
            invoice.setTaxPercentage(request.getTaxPercentage());
        }

        if(request.getCreatedByUserId() != null) {
            util.validateCreatedByUserId(request.getCreatedByUserId());
            User createdBy = userRepository.findById(request.getCreatedByUserId())
                .orElseThrow(() -> new UserNotFoundException(request.getCreatedByUserId()));
            invoice.setCreatedBy(createdBy);
        }

        if(request.getCustomerId() != null) {
            Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new CustomerNotFoundException(request.getCustomerId()));

            invoice.setCustomer(customer);
        }

        if(request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        }

        // Add items
        util.addItems(invoice, request.getItems());

        // Issue only when total amount is greater than 0
        if(request.getIssueDate() != null) {
            if(invoice.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidInvoiceFieldException("issueDate", "must be after the total amount is greater than 0");
            }
            invoice.setIssueDate(request.getIssueDate());
        }
        // Save invoice
        return invoiceRepository.save(invoice);
    }
}
