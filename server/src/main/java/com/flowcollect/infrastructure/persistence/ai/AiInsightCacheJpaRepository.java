package com.flowcollect.infrastructure.persistence.ai;

import com.flowcollect.domain.ai.AiInsightCache;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AiInsightCacheJpaRepository extends JpaRepository<AiInsightCache, UUID> {
    Optional<AiInsightCache> findByOrganizationId(UUID organizationId);
}
