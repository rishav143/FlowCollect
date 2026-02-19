package com.cashclarity.application.invoice;

import com.cashclarity.api.v1.invoice.dto.InvoiceRequest;
import com.cashclarity.api.v1.invoice.dto.IssueInvoiceRequest;
import com.cashclarity.application.customer.CustomerUtil;
import com.cashclarity.application.organization.OrganizationUtil;
import com.cashclarity.application.user.UserUtil;
import com.cashclarity.api.v1.invoice.dto.InvoiceUpdateRequest;
import com.cashclarity.domain.customer.Customer;
import com.cashclarity.domain.invoice.Invoice;
import com.cashclarity.domain.invoice.InvoiceItem;
import com.cashclarity.domain.invoice.LifeCycleStatus;
import com.cashclarity.domain.invoice.TimeStatus;
import com.cashclarity.domain.organization.Organization;
import com.cashclarity.domain.user.User;
import com.cashclarity.exception.invoice.InvoiceAlreadyExistsException;
import com.cashclarity.exception.invoice.InvoiceNotFoundException;
import com.cashclarity.exception.customer.CustomerNotFoundException;
import com.cashclarity.exception.invoice.InvalidInvoiceFieldException;
import com.cashclarity.exception.organization.InvalidOrganizationFieldException;
import com.cashclarity.exception.user.UserNotFoundException;
import com.cashclarity.infrastructure.persistence.customer.CustomerJpaRepository;
import com.cashclarity.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.cashclarity.infrastructure.persistence.organization.OrganizationJpaRepository;
import com.cashclarity.infrastructure.persistence.user.UserJpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Service
public class InvoiceService {

    private final InvoiceJpaRepository invoiceRepository;
    private final OrganizationJpaRepository organizationRepository;
    private final UserJpaRepository userRepository;
    private final CustomerJpaRepository customerRepository;

    public InvoiceService(
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
    public Invoice createInvoice(java.util.UUID organizationId, InvoiceRequest request) {

        // Validate request otherwise throw exception
        if (request == null) {
            throw new InvalidInvoiceFieldException("request must not be null");
        }

        Organization organization = OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);

        String normalizedInvoiceNumber = request.getInvoiceNumber().trim();
        if (invoiceRepository.existsByInvoiceNumberAndOrganizationId(normalizedInvoiceNumber, request.getOrganizationId())) {
            throw new InvoiceAlreadyExistsException("invoice number must be unique within organization");
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
            User user = UserUtil.validateUserWithOrganization(request.getCreatedByUserId(), organizationId, userRepository);
            invoice.setCreatedBy(user);
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
        InvoiceUtil.addItems(invoice, request.getItems());

        // Issue only when total amount is greater than 0
        if(request.getIssueDate() != null) {
            if(invoice.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidInvoiceFieldException("issueDate must be after the total amount is greater than 0");
            }
            invoice.setIssueDate(request.getIssueDate());
        }
        // Save invoice
        return invoiceRepository.save(invoice);
    }

    /**
     * Issues a draft invoice. If request is null or request.issueDate is null,
     * the invoice will be issued using current date. Exceptions are thrown with
     * clear messages and propagated to the controller.
     */
    @org.springframework.transaction.annotation.Transactional
    public Invoice issueInvoice(java.util.UUID organizationId, java.util.UUID invoiceId, IssueInvoiceRequest request) {
        if (invoiceId == null) {
            throw new com.cashclarity.exception.invoice.InvoiceNotFoundException(invoiceId);
        }
        Invoice invoice = InvoiceUtil.validateInvoiceWithOrganization(invoiceId, organizationId, invoiceRepository);

        if (!invoice.isDraft()) {
            throw new InvalidInvoiceFieldException("lifeCycleStatus must be draft to be issued");
        }

        // If explicit date provided, use it; otherwise issue now.
        if (request != null && request.getIssueDate() != null) {
            invoice.setIssueDate(request.getIssueDate());
        } else {
            invoice.setIssuedNow();
        }

        return invoiceRepository.save(invoice);
    }

    /**
     * Gets an invoice by id.
     */
    public Invoice getInvoice(java.util.UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new com.cashclarity.exception.invoice.InvoiceNotFoundException(invoiceId));
    }

    /**
     * Updates mutable fields of a draft invoice.
     * Throws exceptions with clear messages for easier debugging.
     */
    @Transactional
    public Invoice updateInvoice(UUID organizationId, UUID invoiceId, InvoiceUpdateRequest request) {

        if (request == null) {
            throw new InvalidInvoiceFieldException("request must not be null");
        }

        Invoice invoice = InvoiceUtil.validateInvoiceWithOrganization(invoiceId, organizationId, invoiceRepository);

        if (!invoice.isDraft()) {
            throw new InvalidInvoiceFieldException("lifeCycleStatus must be draft to be updated");
        }

        // invoice number update - ensure uniqueness within organization when changed
        if (request.getInvoiceNumber() != null) {
            String normalized = request.getInvoiceNumber().trim();
            if (normalized.isBlank()) {
                throw new InvalidInvoiceFieldException("invoiceNumber must not be blank");
            }
            if (!normalized.equals(invoice.getInvoiceNumber())
                    && invoiceRepository.existsByInvoiceNumberAndOrganizationId(normalized, organizationId)) {
                throw new InvoiceAlreadyExistsException("invoice number must be unique within organization");
            }
            invoice.setInvoiceNumber(normalized);
        }

        // tax percentage
        if (request.getTaxPercentage() != null) {
            invoice.setTaxPercentage(request.getTaxPercentage());
        }

        // createdBy user
        if (request.getCreatedByUserId() != null) {
            User user = UserUtil.validateUserWithOrganization(request.getCreatedByUserId(), organizationId, userRepository);
            invoice.setCreatedBy(user);
        }
        
        // customer
        if (request.getCustomerId() != null) {
            Customer customer = CustomerUtil.validateCustomerWithOrganization(request.getCustomerId(), organizationId, customerRepository);
            invoice.setCustomer(customer);
        }
        
        // due date
        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        }

        // items - replace existing items with provided list
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            // remove existing items safely
            List<InvoiceItem> existing = new java.util.ArrayList<>(invoice.getItems());
            for (com.cashclarity.domain.invoice.InvoiceItem item : existing) {
                invoice.removeItem(item);
            }
            // add new items (util.addItems will validate)
            InvoiceUtil.addItems(invoice, request.getItems());
        }

        // Save and return
        return invoiceRepository.save(invoice);
    }

    /**
     * Hard delete an invoice if and only if it is in DRAFT state and belongs to the organization.
     * Throws clear exceptions to propagate to controller for debugging.
     */
    @Transactional
    public void deleteInvoice(java.util.UUID organizationId, java.util.UUID invoiceId) {
        if (invoiceId == null) {
            throw new InvalidInvoiceFieldException("invoiceId must not be null");
        }

        OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        if (!invoice.getOrganization().getId().equals(organizationId)) {
            throw new InvalidInvoiceFieldException("organizationId invoice does not belong to organization " + organizationId);
        }

        if (!invoice.isDraft()) {
            throw new InvalidInvoiceFieldException("only draft invoices may be deleted");
        }

        // Hard delete - JPA will cascade delete items due to orphanRemoval/cascade settings
        invoiceRepository.delete(invoice);
    }

    /**
    /**
     * Return paginated invoices for an organization with optional filters.
     */
    public Page<Invoice> list(
        java.util.UUID organizationId,
        TimeStatus timeStatus,
        LifeCycleStatus lifeCycleStatus,
        String invoiceNumber,
        LocalDate createdAt,
        LocalDate updatedAt,
        LocalDate dueDate,
        Pageable pageable
    ) {
        OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);

        Specification<Invoice> spec = (root, query, cb) -> {
            Predicate p = cb.equal(root.get("organization").get("id"), organizationId);

            if (timeStatus != null) {
                p = cb.and(p, cb.equal(root.get("timeStatus"), timeStatus));
            }

            if (lifeCycleStatus != null) {
                p = cb.and(p, cb.equal(root.get("lifeCycleStatus"), lifeCycleStatus));
            }

            if (invoiceNumber != null && !invoiceNumber.isBlank()) {
                p = cb.and(p, cb.like(cb.lower(root.get("invoiceNumber")), "%" + invoiceNumber.toLowerCase() + "%"));
            }

            if (createdAt != null) {
                p = cb.and(p, cb.between(root.get("createdAt"),
                        createdAt.atStartOfDay(),
                        createdAt.atTime(LocalTime.MAX)));
            }

            if (updatedAt != null) {
                p = cb.and(p, cb.between(root.get("updatedAt"),
                        updatedAt.atStartOfDay(),
                        updatedAt.atTime(LocalTime.MAX)));
            }

            if (dueDate != null) {
                p = cb.and(p, cb.between(root.get("dueDate"),
                        dueDate.atStartOfDay(),
                        dueDate.atTime(LocalTime.MAX)));
            }

            return p;
        };

        return invoiceRepository.findAll(spec, pageable);
    }

}
