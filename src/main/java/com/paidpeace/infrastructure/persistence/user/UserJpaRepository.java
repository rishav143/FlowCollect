package com.paidpeace.infrastructure.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.paidpeace.domain.user.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserJpaRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<User> findByOrganizationId(UUID id);

    Optional<User> findByEmailAndOrganizationId(String email, UUID organizationId);

    boolean existsByEmailAndOrganizationId(String email, UUID organizationId);

    boolean existsByEmailAndOrganizationIdAndIdNot(String email, UUID organizationId, UUID id);
}
