package com.flowcollect.infrastructure.persistence.template;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.flowcollect.domain.template.Template;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateJpaRepository extends JpaRepository<Template, UUID>, JpaSpecificationExecutor<Template> {
    Optional<Template> findById(UUID id);
    boolean existsByNameAndOrganizationId(String name, UUID organizationId);

    /** Find a system-level template (no org) by name — used by the seeder for idempotency. */
    Optional<Template> findByNameAndOrganizationIsNull(String name);

    /** All platform-level seed templates (org=null, systemDefined=true). */
    List<Template> findBySystemDefinedTrue();

    /** All templates owned by a specific org — used by OrgDefaultDataSeeder. */
    List<Template> findByOrganizationId(UUID organizationId);
}

