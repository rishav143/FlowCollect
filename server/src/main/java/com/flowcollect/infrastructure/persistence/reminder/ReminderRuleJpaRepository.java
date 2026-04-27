package com.flowcollect.infrastructure.persistence.reminder;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.flowcollect.domain.reminder.ReminderRule;
import com.flowcollect.domain.reminder.RuleMode;

public interface ReminderRuleJpaRepository extends JpaRepository<ReminderRule, UUID>, JpaSpecificationExecutor<ReminderRule> {

    List<ReminderRule> findByOrganizationIdAndActiveTrue(UUID organizationId);

    boolean existsByNameAndOrganizationId(String name, UUID organizationId);

    boolean existsByTemplateId(UUID templateId);

    // Returns all active system-defined AUTO rules — used by the recovery engine
    List<ReminderRule> findBySystemDefinedTrueAndActiveTrueAndMode(RuleMode mode);

    /** Seeder idempotency check — finds by name regardless of active state. */
    java.util.Optional<ReminderRule> findByNameAndSystemDefinedTrue(String name);

    /** All platform-level seed rules (org=null, systemDefined=true). */
    List<ReminderRule> findBySystemDefinedTrue();

    /** All rules owned by a specific org — used by OrgDefaultDataSeeder. */
    List<ReminderRule> findByOrganizationId(UUID organizationId);

    /**
     * Returns all active org-owned AUTO rules for a specific org.
     * System-defined rows are blueprints only — each org has its own seeded copies.
     */
    @org.springframework.data.jpa.repository.Query(
        "SELECT r FROM ReminderRule r WHERE r.active = true AND r.mode = 'AUTO' " +
        "AND r.organization.id = :orgId"
    )
    List<ReminderRule> findActiveAutoRulesForOrg(@org.springframework.data.repository.query.Param("orgId") UUID orgId);
}
