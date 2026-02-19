package com.cashclarity.infrastructure.persistence.organization;

import com.cashclarity.domain.organization.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrganizationJpaRepository extends JpaRepository<Organization, UUID>, JpaSpecificationExecutor<Organization> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, UUID id);
}
