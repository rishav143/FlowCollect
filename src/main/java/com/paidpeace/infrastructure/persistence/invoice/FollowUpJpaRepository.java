package com.paidpeace.infrastructure.persistence.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.paidpeace.domain.invoice.followup.FollowUp;
import com.paidpeace.domain.invoice.followup.FollowUpStatus;
import com.paidpeace.domain.invoice.followup.FollowUpTriggerType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface FollowUpJpaRepository extends JpaRepository<FollowUp, UUID>, JpaSpecificationExecutor<FollowUp> {

    boolean existsByInvoiceIdAndReminderRuleIdAndScheduledForDate(UUID invoiceId, UUID reminderRuleId, LocalDate scheduledForDate);

    List<FollowUp> findByStatusAndTriggerType(FollowUpStatus status, FollowUpTriggerType triggerType);

    List<FollowUp> findByStatusAndTriggerTypeAndInvoiceOrganizationId(
            FollowUpStatus status,
            FollowUpTriggerType triggerType,
            UUID organizationId
    );
}

