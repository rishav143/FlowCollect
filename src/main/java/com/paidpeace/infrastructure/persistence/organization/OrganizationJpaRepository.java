package com.paidpeace.infrastructure.persistence.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.paidpeace.domain.organization.Organization;

import java.util.UUID;

@Repository
public interface OrganizationJpaRepository extends JpaRepository<Organization, UUID>, JpaSpecificationExecutor<Organization> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
