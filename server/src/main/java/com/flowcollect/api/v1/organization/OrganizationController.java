package com.flowcollect.api.v1.organization;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.flowcollect.api.v1.organization.dto.BillingResponse;
import com.flowcollect.api.v1.organization.dto.OrganizationCreateRequest;
import com.flowcollect.api.v1.organization.dto.OrganizationResponse;
import com.flowcollect.api.v1.organization.dto.OrganizationUpdateRequest;
import com.flowcollect.api.v1.organization.dto.OrgPaymentDetailsRequest;
import com.flowcollect.api.v1.organization.dto.OrgPaymentDetailsResponse;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.application.organization.OrgPaymentDetailsService;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.flowcollect.security.RequireRole;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService    organizationService;
    private final OrgPaymentDetailsService paymentDetailsService;
    private final InvoiceJpaRepository   invoiceRepository;

    public OrganizationController(
            OrganizationService      organizationService,
            OrgPaymentDetailsService paymentDetailsService,
            InvoiceJpaRepository     invoiceRepository
    ) {
        this.organizationService  = organizationService;
        this.paymentDetailsService = paymentDetailsService;
        this.invoiceRepository    = invoiceRepository;
    }

    // Create a new organization.
    @PostMapping
    @RequireRole({ UserRole.ADMIN })
    public ResponseEntity<OrganizationResponse> create(@Valid @RequestBody OrganizationCreateRequest request) {
        Organization created = organizationService.create(request);
        OrganizationResponse response = OrganizationMapper.toResponse(created);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    // Get an organization by id.
    @GetMapping("/{organizationId}")
    @RequireRole({ UserRole.ADMIN, UserRole.STAFF })
    public ResponseEntity<OrganizationResponse> getById(@PathVariable UUID organizationId) {
        Organization organization = organizationService.getAuthorizedById(organizationId);
        return ResponseEntity.ok(OrganizationMapper.toResponse(organization));
    }

    // List organizations with filters and pagination.
    @GetMapping
    @RequireRole({ UserRole.ADMIN, UserRole.STAFF })
    public ResponseEntity<Page<OrganizationResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            Pageable pageable
    ) {
        Page<Organization> organizations = organizationService.listAuthorized(
                status,
                email,
                name,
                createdFrom,
                createdTo,
                pageable
        );
        return ResponseEntity.ok(organizations.map(OrganizationMapper::toResponse));
    }

    // Update an organization by id.
    @PatchMapping("/{organizationId}")
    @RequireRole({ UserRole.ADMIN })
    public ResponseEntity<OrganizationResponse> update(
            @PathVariable UUID organizationId,
            @Valid @RequestBody OrganizationUpdateRequest request
    ) {
        Organization updated = organizationService.updateAuthorized(organizationId, request);
        return ResponseEntity.ok(OrganizationMapper.toResponse(updated));
    }

    // Get payment receiving details for an org.
    @GetMapping("/{organizationId}/payment-details")
    @RequireRole({ UserRole.ADMIN, UserRole.STAFF })
    public ResponseEntity<OrgPaymentDetailsResponse> getPaymentDetails(@PathVariable UUID organizationId) {
        organizationService.getAuthorizedById(organizationId); // authorization check
        OrgPaymentDetailsResponse response = paymentDetailsService.getByOrgId(organizationId);
        if (response == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(response);
    }

    // Save (create or replace) payment receiving details for an org.
    @PutMapping("/{organizationId}/payment-details")
    @RequireRole({ UserRole.ADMIN })
    public ResponseEntity<OrgPaymentDetailsResponse> savePaymentDetails(
            @PathVariable UUID organizationId,
            @Valid @RequestBody OrgPaymentDetailsRequest request
    ) {
        organizationService.getAuthorizedById(organizationId); // authorization check
        OrgPaymentDetailsResponse response = paymentDetailsService.saveForOrg(organizationId, request);
        return ResponseEntity.ok(response);
    }

    // Get billing info for an org (plan, subscription status, expiry).
    @GetMapping("/{organizationId}/billing")
    @RequireRole({ UserRole.ADMIN, UserRole.STAFF })
    public ResponseEntity<BillingResponse> getBilling(@PathVariable UUID organizationId) {
        Organization org         = organizationService.getAuthorizedById(organizationId);
        long         activeCount = invoiceRepository.countActiveByOrganizationId(organizationId);
        return ResponseEntity.ok(new BillingResponse(
                org.getPlan(),
                org.getStatus(),
                org.getPlanExpiresAt(),
                org.getDodoSubscriptionId() != null,
                activeCount
        ));
    }

    // Hard-delete an organization by id.
    @DeleteMapping("/{organizationId}")
    @RequireRole({ UserRole.ADMIN })
    public ResponseEntity<Void> delete(@PathVariable UUID organizationId) {
        organizationService.deleteAuthorized(organizationId);
        return ResponseEntity.noContent().build();
    }

    // Activate an organization by id.
    @PostMapping("/{organizationId}/activate")
    @RequireRole({ UserRole.ADMIN })
    public ResponseEntity<OrganizationResponse> activate(@PathVariable UUID organizationId) {
        Organization activated = organizationService.activateAuthorized(organizationId);
        return ResponseEntity.ok(OrganizationMapper.toResponse(activated));
    }

    // Suspend an organization by id.
    @PostMapping("/{organizationId}/suspend")
    @RequireRole({ UserRole.ADMIN })
    public ResponseEntity<OrganizationResponse> suspend(@PathVariable UUID organizationId) {
        Organization suspended = organizationService.suspendAuthorized(organizationId);
        return ResponseEntity.ok(OrganizationMapper.toResponse(suspended));
    }

    // Archive an organization by id.
    @PostMapping("/{organizationId}/archive")
    @RequireRole({ UserRole.ADMIN })
    public ResponseEntity<OrganizationResponse> archive(@PathVariable UUID organizationId) {
        Organization archived = organizationService.archiveAuthorized(organizationId);
        return ResponseEntity.ok(OrganizationMapper.toResponse(archived));
    }
}


