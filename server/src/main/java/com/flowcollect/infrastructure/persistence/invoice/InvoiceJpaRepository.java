package com.flowcollect.infrastructure.persistence.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.LifeCycleStatus;
import com.flowcollect.domain.invoice.TimeStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceJpaRepository extends JpaRepository<Invoice, UUID>, JpaSpecificationExecutor<Invoice> {

    boolean existsByInvoiceNumberAndOrganizationId(String invoiceNumber, UUID organizationId);

    boolean existsByCustomer_Id(UUID customerId);

    List<Invoice> findAllByOrganizationIdAndLifeCycleStatusInAndDueDate(UUID organizationId, List<LifeCycleStatus> statuses, LocalDate dueDate);

    List<Invoice> findAllByOrganizationIdAndLifeCycleStatusInAndDueDateIsNotNull(UUID organizationId, List<LifeCycleStatus> statuses);

    Optional<Invoice> findByIdAndOrganizationId(UUID invoiceId, UUID orgId);

    /** Counts active (non-terminal) invoices for an org — used to enforce STARTER plan limit. */
    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.organization.id = :orgId AND i.lifeCycleStatus IN ('DRAFT', 'ISSUED', 'PARTIALLY_PAID')")
    long countActiveByOrganizationId(@Param("orgId") UUID orgId);

    /** Fetches overdue invoices with customer eagerly loaded — avoids N+1 when mapping customer fields. */
    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.customer WHERE i.organization.id = :orgId AND i.timeStatus = :timeStatus AND i.lifeCycleStatus IN :statuses")
    List<Invoice> findByOrgAndTimeStatusAndLifeCycleStatusInWithCustomer(
            @Param("orgId") UUID orgId,
            @Param("timeStatus") TimeStatus timeStatus,
            @Param("statuses") List<LifeCycleStatus> statuses);
}
