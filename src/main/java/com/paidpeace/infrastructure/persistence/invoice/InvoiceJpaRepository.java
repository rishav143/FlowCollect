package com.paidpeace.infrastructure.persistence.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.paidpeace.domain.invoice.Invoice;

import java.util.UUID;

@Repository
public interface InvoiceJpaRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    boolean existsByInvoiceNumberAndOrganizationId(String invoiceNumber, UUID organizationId);
}
