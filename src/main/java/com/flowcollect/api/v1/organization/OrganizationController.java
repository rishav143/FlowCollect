package com.flowcollect.api.v1.organization;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.flowcollect.api.v1.organization.dto.OrganizationCreateRequest;
import com.flowcollect.api.v1.organization.dto.OrganizationResponse;
import com.flowcollect.api.v1.organization.dto.OrganizationUpdateRequest;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.security.RequireRole;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
@RequireRole({ UserRole.ADMIN })
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    // Create a new organization.
    @PostMapping
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
    public ResponseEntity<OrganizationResponse> getById(@PathVariable UUID organizationId) {
        Organization organization = organizationService.getById(organizationId);
        return ResponseEntity.ok(OrganizationMapper.toResponse(organization));
    }

    // List organizations with filters and pagination.
    @GetMapping
    public ResponseEntity<Page<OrganizationResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            Pageable pageable
    ) {
        Page<Organization> organizations = organizationService.list(
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
    public ResponseEntity<OrganizationResponse> update(
            @PathVariable UUID organizationId,
            @Valid @RequestBody OrganizationUpdateRequest request
    ) {
        Organization updated = organizationService.update(organizationId, request);
        return ResponseEntity.ok(OrganizationMapper.toResponse(updated));
    }

    // Hard-delete an organization by id.
    @DeleteMapping("/{organizationId}")
    public ResponseEntity<Void> delete(@PathVariable UUID organizationId) {
        organizationService.delete(organizationId);
        return ResponseEntity.noContent().build();
    }

    // Activate an organization by id.
    @PostMapping("/{organizationId}/activate")
    public ResponseEntity<OrganizationResponse> activate(@PathVariable UUID organizationId) {
        Organization activated = organizationService.activate(organizationId);
        return ResponseEntity.ok(OrganizationMapper.toResponse(activated));
    }

    // Suspend an organization by id.
    @PostMapping("/{organizationId}/suspend")
    public ResponseEntity<OrganizationResponse> suspend(@PathVariable UUID organizationId) {
        Organization suspended = organizationService.suspend(organizationId);
        return ResponseEntity.ok(OrganizationMapper.toResponse(suspended));
    }

    // Archive an organization by id.
    @PostMapping("/{organizationId}/archive")
    public ResponseEntity<OrganizationResponse> archive(@PathVariable UUID organizationId) {
        Organization archived = organizationService.archive(organizationId);
        return ResponseEntity.ok(OrganizationMapper.toResponse(archived));
    }
}


