package com.flowcollect.api.v1.billing;

import com.flowcollect.api.v1.organization.dto.BillingResponse;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.domain.organization.OrgPlan;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.flowcollect.infrastructure.persistence.organization.OrganizationJpaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * DEV-ONLY billing simulation endpoints.
 *
 * Only active when SPRING_PROFILES_ACTIVE=local.
 * Never compiled out of production — Spring just won't register these beans
 * unless the "local" profile is active.
 *
 * Usage:
 *   POST /api/v1/dev/organizations/{orgId}/billing/simulate/activate   → PRO for 30 days
 *   POST /api/v1/dev/organizations/{orgId}/billing/simulate/cancel     → cancelled (PRO until expiry)
 *   POST /api/v1/dev/organizations/{orgId}/billing/simulate/expire     → STARTER
 *   POST /api/v1/dev/organizations/{orgId}/billing/simulate/hold       → SUSPENDED
 *   GET  /api/v1/dev/organizations/{orgId}/billing                     → current billing state
 */
@RestController
@RequestMapping("/api/v1/dev/organizations/{organizationId}/billing")
@Profile("local")
public class DevBillingController {

    private final OrganizationService         organizationService;
    private final OrganizationJpaRepository   organizationRepository;
    private final InvoiceJpaRepository        invoiceRepository;

    public DevBillingController(
            OrganizationService       organizationService,
            OrganizationJpaRepository organizationRepository,
            InvoiceJpaRepository      invoiceRepository
    ) {
        this.organizationService  = organizationService;
        this.organizationRepository = organizationRepository;
        this.invoiceRepository    = invoiceRepository;
    }

    /** Simulate subscription.active — upgrades org to PRO for 30 days. */
    @PostMapping("/simulate/activate")
    @Transactional
    public ResponseEntity<BillingResponse> simulateActivate(@PathVariable UUID organizationId) {
        Organization org = organizationService.getById(organizationId);
        org.activatePlan(
                OrgPlan.PRO,
                "dev-sub-" + UUID.randomUUID(),
                "dev-customer-" + organizationId,
                Instant.now().plus(30, ChronoUnit.DAYS)
        );
        organizationRepository.save(org);
        return billing(org, organizationId);
    }

    /** Simulate subscription.cancelled — keeps PRO but marks subscription as cancelled. */
    @PostMapping("/simulate/cancel")
    @Transactional
    public ResponseEntity<BillingResponse> simulateCancel(@PathVariable UUID organizationId) {
        Organization org = organizationService.getById(organizationId);
        org.markSubscriptionCancelled();
        organizationRepository.save(org);
        return billing(org, organizationId);
    }

    /** Simulate subscription.expired — downgrades back to STARTER. */
    @PostMapping("/simulate/expire")
    @Transactional
    public ResponseEntity<BillingResponse> simulateExpire(@PathVariable UUID organizationId) {
        Organization org = organizationService.getById(organizationId);
        org.expirePlan();
        organizationRepository.save(org);
        return billing(org, organizationId);
    }

    /** Simulate subscription.on_hold — suspends the org. */
    @PostMapping("/simulate/hold")
    @Transactional
    public ResponseEntity<BillingResponse> simulateHold(@PathVariable UUID organizationId) {
        Organization org = organizationService.getById(organizationId);
        org.holdPlan();
        organizationRepository.save(org);
        return billing(org, organizationId);
    }

    /** Returns current billing state — useful to verify DB after simulation. */
    @GetMapping
    public ResponseEntity<BillingResponse> getBilling(@PathVariable UUID organizationId) {
        Organization org = organizationService.getById(organizationId);
        return billing(org, organizationId);
    }

    private ResponseEntity<BillingResponse> billing(Organization org, UUID organizationId) {
        long activeCount = invoiceRepository.countActiveByOrganizationId(organizationId);
        return ResponseEntity.ok(new BillingResponse(
                org.getPlan(),
                org.getStatus(),
                org.getPlanExpiresAt(),
                org.getDodoSubscriptionId() != null,
                activeCount
        ));
    }
}
