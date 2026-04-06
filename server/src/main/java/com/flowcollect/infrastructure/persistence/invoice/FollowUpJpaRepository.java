package com.flowcollect.infrastructure.persistence.invoice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    List<FollowUp> findByInvoiceIdAndStatus(UUID invoiceId, FollowUpStatus status);

    List<FollowUp> findByStatusAndInvoiceOrganizationId(FollowUpStatus status, UUID organizationId);

    java.util.Optional<FollowUp> findByResendEmailId(String resendEmailId);

    @Modifying
    @Query("UPDATE FollowUp f SET f.template = null WHERE f.template.id = :templateId")
    void detachTemplate(@Param("templateId") UUID templateId);

    @Modifying
    @Query("UPDATE FollowUp f SET f.reminderRule = null WHERE f.reminderRule.id = :ruleId")
    void detachReminderRule(@Param("ruleId") UUID ruleId);

    /** Count SENT AUTO follow-ups dispatched today for an org (used for Recover KPI). */
    @Query("SELECT COUNT(f) FROM FollowUp f WHERE f.invoice.organization.id = :orgId " +
           "AND f.triggerType = 'AUTOMATED' AND f.reminderRule.mode = 'AUTO' " +
           "AND f.status = 'SENT' AND f.sentAt >= :dayStart AND f.sentAt < :dayEnd")
    long countSentAutoToday(
        @Param("orgId") UUID orgId,
        @Param("dayStart") java.time.Instant dayStart,
        @Param("dayEnd") java.time.Instant dayEnd
    );

    /** Count PENDING AUTO follow-ups queued for an org (used for Recover KPI). */
    @Query("SELECT COUNT(f) FROM FollowUp f WHERE f.invoice.organization.id = :orgId " +
           "AND f.triggerType = 'AUTOMATED' AND f.reminderRule.mode = 'AUTO' " +
           "AND f.status = 'PENDING'")
    long countPendingAuto(@Param("orgId") UUID orgId);

    /** Count SENT AUTO follow-ups dispatched this week for an org (used for Recover banner). */
    @Query("SELECT COUNT(f) FROM FollowUp f WHERE f.invoice.organization.id = :orgId " +
           "AND f.triggerType = 'AUTOMATED' AND f.reminderRule.mode = 'AUTO' " +
           "AND f.status = 'SENT' AND f.sentAt >= :weekStart AND f.sentAt < :weekEnd")
    long countSentAutoThisWeek(
        @Param("orgId") UUID orgId,
        @Param("weekStart") java.time.Instant weekStart,
        @Param("weekEnd") java.time.Instant weekEnd
    );

    /** PENDING AUTO follow-ups for org — used for Today's Send Queue panel. */
    @Query("SELECT f FROM FollowUp f " +
           "JOIN FETCH f.invoice i " +
           "LEFT JOIN FETCH i.customer " +
           "WHERE i.organization.id = :orgId " +
           "AND f.triggerType = 'AUTOMATED' AND f.reminderRule.mode = 'AUTO' " +
           "AND f.status = 'PENDING' " +
           "ORDER BY f.scheduledForDate ASC, f.createdAt ASC")
    List<FollowUp> findPendingAutoQueue(@Param("orgId") UUID orgId);

    /** Recent SENT or CANCELLED AUTO follow-ups for org — used for Activity Log panel. */
    @Query("SELECT f FROM FollowUp f " +
           "JOIN FETCH f.invoice i " +
           "LEFT JOIN FETCH i.customer " +
           "LEFT JOIN FETCH f.reminderRule " +
           "WHERE i.organization.id = :orgId " +
           "AND f.triggerType = 'AUTOMATED' AND f.reminderRule.mode = 'AUTO' " +
           "AND f.status IN ('SENT', 'CANCELLED') " +
           "AND f.updatedAt >= :since " +
           "ORDER BY f.updatedAt DESC")
    org.springframework.data.domain.Slice<FollowUp> findRecentAutoActivity(
        @Param("orgId") UUID orgId,
        @Param("since") java.time.Instant since,
        org.springframework.data.domain.Pageable pageable
    );

    /** Any SENT AUTO follow-up for a given invoice — used to tag recovered payments. */
    @Query("SELECT f FROM FollowUp f WHERE f.invoice.id = :invoiceId " +
           "AND f.triggerType = 'AUTOMATED' AND f.reminderRule.mode = 'AUTO' " +
           "AND f.status = 'SENT' ORDER BY f.sentAt DESC")
    java.util.Optional<FollowUp> findFirstSentAutoFollowUpByInvoiceId(@Param("invoiceId") UUID invoiceId);
}

