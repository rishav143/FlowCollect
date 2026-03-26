package com.flowcollect.infrastructure.persistence.invoice;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.flowcollect.domain.invoice.payment.Payment;

@Repository
    public interface PaymentJpaRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {
        List<Payment> findByInvoiceId(UUID invoiceId);

        /** Used by PaymentLinkService to prevent double-recording a webhook payment. */
        boolean existsByInvoiceIdAndReferenceId(UUID invoiceId, String referenceId);
    }