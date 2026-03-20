package com.flowcollect.infrastructure.persistence.verification;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flowcollect.domain.verification.PhoneOtp;

public interface PhoneOtpJpaRepository extends JpaRepository<PhoneOtp, UUID> {

    Optional<PhoneOtp> findTopByOrganizationIdAndUsedFalseOrderByCreatedAtDesc(UUID organizationId);

    void deleteByOrganizationId(UUID organizationId);
}
