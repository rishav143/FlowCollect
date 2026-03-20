package com.flowcollect.infrastructure.persistence.verification;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flowcollect.domain.verification.EmailVerificationToken;

public interface EmailVerificationTokenJpaRepository extends JpaRepository<EmailVerificationToken, UUID> {

    Optional<EmailVerificationToken> findByToken(String token);

    void deleteByUserId(UUID userId);
}
