package com.flowcollect.infrastructure.persistence.organization;

import com.flowcollect.domain.organization.OrgPaymentDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrgPaymentDetailsRepository extends JpaRepository<OrgPaymentDetails, UUID> {
    Optional<OrgPaymentDetails> findByOrgId(UUID orgId);
}
