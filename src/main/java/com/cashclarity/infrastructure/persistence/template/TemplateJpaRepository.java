package com.cashclarity.infrastructure.persistence.template;

import com.cashclarity.domain.template.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TemplateJpaRepository extends JpaRepository<Template, UUID> {
    // find methods can be added later if required
}

