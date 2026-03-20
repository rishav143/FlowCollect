package com.flowcollect.infrastructure.persistence.reminder;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.flowcollect.domain.reminder.ReminderRule;

public interface ReminderRuleJpaRepository extends JpaRepository<ReminderRule, UUID>, JpaSpecificationExecutor<ReminderRule> {

    List<ReminderRule> findByOrganizationIdAndActiveTrue(UUID organizationId);

    boolean existsByNameAndOrganizationId(String name, UUID organizationId);
}
