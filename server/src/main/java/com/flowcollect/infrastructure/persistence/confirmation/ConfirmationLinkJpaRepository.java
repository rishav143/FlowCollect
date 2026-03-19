package com.flowcollect.infrastructure.persistence.confirmation;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flowcollect.domain.confirmation.ConfirmationLink;

public interface ConfirmationLinkJpaRepository extends JpaRepository<ConfirmationLink, UUID> {

    Optional<ConfirmationLink> findByToken(String token);

    Optional<ConfirmationLink> findByInvoiceId(UUID invoiceId);
}
