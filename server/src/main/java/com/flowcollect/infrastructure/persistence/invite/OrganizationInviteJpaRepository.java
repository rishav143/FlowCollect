package com.flowcollect.infrastructure.persistence.invite;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.flowcollect.domain.invite.InviteStatus;
import com.flowcollect.domain.invite.OrganizationInvite;

public interface OrganizationInviteJpaRepository extends JpaRepository<OrganizationInvite, UUID> {

    Optional<OrganizationInvite> findByToken(String token);

    List<OrganizationInvite> findByOrganizationIdAndStatus(UUID organizationId, InviteStatus status);

    boolean existsByOrganizationIdAndEmailAndStatus(UUID organizationId, String email, InviteStatus status);

    Optional<OrganizationInvite> findByOrganizationIdAndEmailAndStatus(
            UUID organizationId, String email, InviteStatus status);
}
