package com.flowcollect.infrastructure.persistence.template;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.flowcollect.domain.template.Template;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateJpaRepository extends JpaRepository<Template, UUID>, JpaSpecificationExecutor<Template> {
    Optional<Template> findById(UUID id);
    boolean existsByNameAndOrganizationId(String name, UUID organizationId);
}

