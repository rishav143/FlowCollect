package com.flowcollect.infrastructure.persistence.organization;

import com.flowcollect.domain.invoice.paymentlink.PaymentGateway;
import com.flowcollect.domain.organization.gateway.GatewayConnectionStatus;
import com.flowcollect.domain.organization.gateway.OrganizationGatewayConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationGatewayConfigRepository extends JpaRepository<OrganizationGatewayConfig, UUID> {

    /** Find the config for a specific org + gateway combination. */
    Optional<OrganizationGatewayConfig> findByOrganizationIdAndGateway(UUID organizationId, PaymentGateway gateway);

    /** List all gateway configs for an organization (across all gateways). */
    List<OrganizationGatewayConfig> findByOrganizationId(UUID organizationId);

    /** Find all CONNECTED configs for a gateway — useful for platform-level reporting. */
    List<OrganizationGatewayConfig> findByGatewayAndStatus(PaymentGateway gateway, GatewayConnectionStatus status);

    /** Used by Stripe webhook to find the org that owns a connected account. */
    Optional<OrganizationGatewayConfig> findByStripeAccountId(String stripeAccountId);
}
