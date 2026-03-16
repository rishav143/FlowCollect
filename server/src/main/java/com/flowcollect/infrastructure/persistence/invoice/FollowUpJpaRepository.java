package com.flowcollect.infrastructure.persistence.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.flowcollect.domain.invoice.followup.FollowUp;
import com.flowcollect.domain.invoice.followup.FollowUpStatus;
import com.flowcollect.domain.invoice.followup.FollowUpTriggerType;

import java.util.List;
import java.util.UUID;

@Repository
public interface FollowUpJpaRepository extends JpaRepository<FollowUp, UUID>, JpaSpecificationExecutor<FollowUp> {

    boolean existsByInvoiceIdAndReminderRuleIdAndOccurrenceIndex(UUID invoiceId, UUID reminderRuleId, int occurrenceIndex);

    List<FollowUp> findByStatusAndTriggerType(FollowUpStatus status, FollowUpTriggerType triggerType);

    List<FollowUp> findByStatusAndTriggerTypeAndInvoiceOrganizationId(
            FollowUpStatus status,
            FollowUpTriggerType triggerType,
            UUID organizationId
    );
}

