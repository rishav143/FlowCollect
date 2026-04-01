package com.flowcollect.infrastructure.persistence.invoice;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.flowcollect.domain.invoice.payment.Payment;

@Repository
public interface PaymentJpaRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {

    List<Payment> findByInvoiceId(UUID invoiceId);

    /** Used by PaymentLinkService to prevent double-recording a webhook payment. */
    boolean existsByInvoiceIdAndReferenceId(UUID invoiceId, String referenceId);

    /**
     * Sum of all payments attributed to the Recover engine for an org.
     * Returns null when no rows match — callers must treat null as zero.
     */
    @Query("SELECT SUM(p.amount) FROM Payment p " +
           "WHERE p.invoice.organization.id = :orgId AND p.recoveredByAutoRuleId IS NOT NULL")
    BigDecimal sumRecoveredAmountByOrgId(@Param("orgId") UUID orgId);
}