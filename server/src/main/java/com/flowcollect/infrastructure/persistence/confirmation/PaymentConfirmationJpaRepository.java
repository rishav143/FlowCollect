package com.flowcollect.infrastructure.persistence.confirmation;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import com.flowcollect.domain.confirmation.PaymentConfirmation;
import com.flowcollect.domain.confirmation.PaymentConfirmationStatus;

public interface PaymentConfirmationJpaRepository extends JpaRepository<PaymentConfirmation, UUID> {

    boolean existsByConfirmationLinkIdAndStatus(UUID confirmationLinkId, PaymentConfirmationStatus status);

    List<PaymentConfirmation> findByConfirmationLinkIdAndStatus(UUID confirmationLinkId, PaymentConfirmationStatus status);

    Page<PaymentConfirmation> findByConfirmationLinkInvoiceOrganizationId(UUID organizationId, Pageable pageable);

    Page<PaymentConfirmation> findByConfirmationLinkInvoiceOrganizationIdAndStatus(
            UUID organizationId, PaymentConfirmationStatus status, Pageable pageable);
}
