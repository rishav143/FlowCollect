package com.cashclarity.application.invoice;

import com.cashclarity.application.util;
import com.cashclarity.domain.invoice.Invoice;
import com.cashclarity.domain.invoice.LifeCycleStatus;
import com.cashclarity.domain.invoice.TimeStatus;
import com.cashclarity.exception.invoice.InvoiceNotFoundException;
import com.cashclarity.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.cashclarity.infrastructure.persistence.organization.OrganizationJpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;

@Service
public class InvoiceQueryService {

    private final InvoiceJpaRepository invoiceRepository;
    private final OrganizationJpaRepository organizationRepository;

    public InvoiceQueryService(InvoiceJpaRepository invoiceRepository, OrganizationJpaRepository organizationRepository) {
        this.invoiceRepository = invoiceRepository;
        this.organizationRepository = organizationRepository;
    }

    public Invoice getInvoice(Long id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new InvoiceNotFoundException(id));
    }

    /**
     * Return paginated invoices for an organization with optional filters.
     */
    public Page<Invoice> list(
        Long organizationId,
        TimeStatus timeStatus,
        LifeCycleStatus lifeCycleStatus,
        String invoiceNumber,
        LocalDate createdAt,
        LocalDate updatedAt,
        LocalDate dueDate,
        Pageable pageable
    ) {
        util.validateOrganizationId(organizationId);
        util.getOrganizationOrThrow(organizationId, organizationRepository);

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
