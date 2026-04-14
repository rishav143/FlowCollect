package com.flowcollect.infrastructure.persistence.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.organization.OrganizationStatus;

import java.util.UUID;

@Repository
public interface OrganizationJpaRepository extends JpaRepository<Organization, UUID>, JpaSpecificationExecutor<Organization> {

    Optional<Organization> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);

    List<Organization> findAllByDeletedAtIsNullAndStatusIn(Collection<OrganizationStatus> statuses);

    Optional<Organization> findByDodoSubscriptionId(String dodoSubscriptionId);
}
