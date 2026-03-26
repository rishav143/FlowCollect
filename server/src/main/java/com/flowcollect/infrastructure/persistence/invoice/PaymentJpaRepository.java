package com.flowcollect.infrastructure.persistence.invoice;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.flowcollect.domain.invoice.payment.Payment;

@Repository
public interface PaymentJpaRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {

    List<Payment> findByInvoiceId(UUID invoiceId);

    /** Used by PaymentLinkService to prevent double-recording a webhook payment. */
    boolean existsByInvoiceIdAndReferenceId(UUID invoiceId, String referenceId);

    /**
     * Sum of all payment amounts for an organization within a time window.
     * Uses paidAt (the actual payment timestamp) so partial payments are included
     * as soon as they are recorded, not only when the invoice is fully paid.
     */
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
           "WHERE p.invoice.organization.id = :orgId " +
           "AND p.paidAt >= :from AND p.paidAt < :to")
    BigDecimal sumAmountByOrganizationAndPaidAtBetween(
        @Param("orgId")  UUID    orgId,
        @Param("from")   Instant from,
        @Param("to")     Instant to
    );
}