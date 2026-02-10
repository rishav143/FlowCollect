package com.cashclarity.infrastructure.persistence.user;

import com.cashclarity.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserJpaRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByIdAndOrganizationId(Long id, Long organizationId);

    Optional<User> findByEmailAndOrganizationId(String email, Long organizationId);

    boolean existsByEmailAndOrganizationId(String email, Long organizationId);
}
