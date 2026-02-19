package com.cashclarity.infrastructure.persistence.invoice;

import com.cashclarity.domain.invoice.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InvoiceJpaRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    boolean existsByInvoiceNumberAndOrganizationId(String invoiceNumber, UUID organizationId);
}
