package com.flowcollect.application.invoice;

import com.flowcollect.exception.http.ForbiddenException;
import com.flowcollect.security.AuthContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowcollect.api.v1.invoice.dto.InvoiceRequest;
import com.flowcollect.api.v1.invoice.dto.InvoiceUpdateRequest;
import com.flowcollect.api.v1.invoice.dto.IssueInvoiceRequest;
import com.flowcollect.application.customer.CustomerService;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.application.user.UserService;
import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.InvoiceItem;
import com.flowcollect.domain.invoice.LifeCycleStatus;
import com.flowcollect.domain.invoice.TimeStatus;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.user.User;
import com.flowcollect.exception.http.NotFoundException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.pdf.InvoicePdfGenerator;
import com.flowcollect.infrastructure.persistence.invoice.InvoiceJpaRepository;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class InvoiceService {

    private final InvoiceJpaRepository invoiceRepository;
    private final UserService userService;
    private final CustomerService customerService;
    private final OrganizationService organizationService;
    private final InvoicePdfGenerator invoicePdfGenerator;
    
    public InvoiceService(
            InvoiceJpaRepository invoiceRepository,
            UserService userService,
            CustomerService customerService,
            OrganizationService organizationService,
            InvoicePdfGenerator invoicePdfGenerator
    ) {
        this.invoiceRepository = invoiceRepository;
        this.userService = userService;
        this.organizationService = organizationService;
        this.customerService = customerService;
        this.invoicePdfGenerator = invoicePdfGenerator;
    }

    // Create a draft invoice.
    @Transactional
    public Invoice createInvoice
    (
        UUID organizationId,
        InvoiceRequest request
    ) {
        // Validate request otherwise throw exception
        if (request == null) {
            throw new ValidationException("Request must not be null");
        }
        Organization organization = organizationService.getById(organizationId);
        String normalizedInvoiceNumber = request.getInvoiceNumber().trim();
        if (invoiceRepository.existsByInvoiceNumberAndOrganizationId(normalizedInvoiceNumber, organizationId)) {
            throw new ValidationException(
                "Invoice number must be unique within organization. Invoice number: " + normalizedInvoiceNumber + " already exists.");
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
            User user = userService.getById(organizationId, request.getCreatedByUserId());
            invoice.setCreatedBy(user);
        }
        if(request.getCustomerId() != null) {
            Customer customer = customerService.getCustomerById(organizationId, request.getCustomerId());
            invoice.setCustomer(customer);
        }
        if(request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
            invoice.refreshTimeStatus(LocalDate.now(organization.getTimezone()));
        }
        InvoiceUtil.addItems(invoice, request.getItems());
        // Issue only when total amount is greater than 0
        if(request.getIssueDate() != null) {
            if(invoice.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException("Issue date must be after the total amount is greater than 0");
            }
            invoice.setIssueDate(request.getIssueDate());
        }

        return invoiceRepository.save(invoice);
    }

    // Issue a draft invoice. If request is null or request.issueDate is null, 
    // the invoice will be issued using current date.
    @Transactional
    public Invoice issueInvoice
    (
        UUID organizationId, 
        UUID invoiceId, 
        IssueInvoiceRequest request
    ) {
        if (invoiceId == null) {
            throw new NotFoundException("Invoice not found with ID: " + invoiceId);
        }
        Organization organization = organizationService.getById(organizationId);
        Invoice invoice = InvoiceUtil.validateInvoiceWithOrganization(invoiceId, organizationId, invoiceRepository);

        if (!invoice.isDraft()) {
            throw new ValidationException(
                "Invoice must be in draft state to be issued. Invoice with ID: " + invoiceId + " is not in draft state.");
        }

        // If explicit date provided, use it; otherwise issue now.
        if (request != null && request.getIssueDate() != null) {
            invoice.setIssueDate(request.getIssueDate());
        } else {
            invoice.setIssuedNow(organization.getTimezone());
        }

        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesForReminders(UUID organizationId, List<LifeCycleStatus> statuses, LocalDate dueDate) {
        return invoiceRepository.findAllByOrganizationIdAndLifeCycleStatusInAndDueDate(organizationId, statuses, dueDate);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesWithDueDate(UUID organizationId, List<LifeCycleStatus> statuses) {
        return invoiceRepository.findAllByOrganizationIdAndLifeCycleStatusInAndDueDateIsNotNull(organizationId, statuses);
    }

    @Transactional
    public void saveAll(List<Invoice> invoices) {
        invoiceRepository.saveAll(invoices);
    }

    // Get invoice by id with organization context
    public Invoice getInvoiceById(UUID organizationId, UUID invoiceId) {
        organizationService.getById(organizationId);
        return InvoiceUtil.validateInvoiceWithOrganization(invoiceId, organizationId, invoiceRepository);
    }

    // Get invoice by id without organization context
    public Invoice getInvoiceById(UUID invoiceId) {
        Invoice invoice = InvoiceUtil.getInvoiceOrThrow(invoiceId, invoiceRepository);
        organizationService.getById(invoice.getOrganization().getId());
        return invoice;
    }

    @Transactional(readOnly = true)
    public InvoicePdfFile generateInvoicePdf(UUID organizationId, UUID invoiceId) {
        Invoice invoice = getInvoiceById(organizationId, invoiceId);
        return new InvoicePdfFile(
                invoicePdfGenerator.generate(invoice),
                invoicePdfGenerator.buildFileName(invoice)
        );
    }

    // Update mutable fields of a draft invoice.
    @Transactional
    public Invoice updateInvoice(
        UUID organizationId, 
        UUID invoiceId, 
        InvoiceUpdateRequest request
    ) {
        if (request == null) {
            throw new ValidationException("Request must not be null");
        }
        organizationService.getById(organizationId);
        Invoice invoice = InvoiceUtil.validateInvoiceWithOrganization(invoiceId, organizationId, invoiceRepository);
        if (!invoice.isDraft()) {
            throw new ValidationException(
                "Invoice must be in draft state to be updated. Invoice with ID: " + invoiceId + " is not in draft state.");
        }
        // invoice number update - ensure uniqueness within organization when changed
        if (request.getInvoiceNumber() != null) {
            String normalized = request.getInvoiceNumber().trim();
            if (normalized.isBlank()) {
                throw new ValidationException("Invoice number must not be blank");
            }
            if (!normalized.equals(invoice.getInvoiceNumber())
                    && invoiceRepository.existsByInvoiceNumberAndOrganizationId(normalized, organizationId)) {
                throw new ValidationException("Invoice number must be unique within organization");
            }
            invoice.setInvoiceNumber(normalized);
        }

        if (request.getTaxPercentage() != null) {
            invoice.setTaxPercentage(request.getTaxPercentage());
        }
        if (request.getCreatedByUserId() != null) {
            User user = userService.getById(organizationId, request.getCreatedByUserId());
            invoice.setCreatedBy(user);
        }
        if (request.getCustomerId() != null) {
            Customer customer = customerService.getCustomerById(organizationId, request.getCustomerId());
            invoice.setCustomer(customer);
        }
        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        }
        if (request.getItems() != null && !request.getItems().isEmpty()) {
            List<InvoiceItem> existing = new ArrayList<>(invoice.getItems());
            for (com.flowcollect.domain.invoice.InvoiceItem item : existing) {
                invoice.removeItem(item);
            }
            InvoiceUtil.addItems(invoice, request.getItems());
        }

        // Save and return
        return invoiceRepository.save(invoice);
    }

    // Hard delete an invoice if and only if it is in DRAFT state and belongs to the organization.
    @Transactional
    public void deleteInvoice(
        UUID organizationId, 
        UUID invoiceId
    ) {
        if (invoiceId == null) {
            throw new ValidationException("Invoice ID must not be null");
        }
        organizationService.getById(organizationId);
        Invoice invoice = InvoiceUtil.validateInvoiceWithOrganization(invoiceId, organizationId, invoiceRepository);
        if (!invoice.isDraft()) {
            throw new ValidationException(
                "Invoice must be in draft state to be deleted. Invoice with ID: " + invoiceId + " is not in draft state.");
        }

        // Hard delete - JPA will cascade delete items due to orphanRemoval/cascade settings
        invoiceRepository.delete(invoice);
    }

    // Return paginated invoices for an organization with optional filters.
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
        organizationService.getById(organizationId);

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
