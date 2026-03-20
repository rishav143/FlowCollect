package com.flowcollect.infrastructure.persistence.verification;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flowcollect.domain.verification.PasswordResetToken;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteByUserId(UUID userId);
}
