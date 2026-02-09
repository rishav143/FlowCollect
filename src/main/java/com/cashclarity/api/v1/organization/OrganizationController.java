package com.cashclarity.api.v1.organization;

import com.cashclarity.api.v1.organization.dto.OrganizationCreateRequest;
import com.cashclarity.api.v1.organization.dto.OrganizationUpdateRequest;
import com.cashclarity.api.v1.organization.dto.OrganizationResponse;
import com.cashclarity.application.organization.OrganizationService;
import com.cashclarity.domain.organization.Organization;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    /**
     * Creates a new organization.
     * Request body is validated by Bean Validation; service layer validates timezone,
     * currency, and duplicate email, throwing exceptions that are handled by {@link com.cashclarity.exception.GlobalExceptionHandler}.
     */
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

    /**
     * Lists organizations with filters and pagination.
     * Service layer validates filters and pagination; exceptions are handled by
     * {@link com.cashclarity.exception.GlobalExceptionHandler}.
     */
    @GetMapping
    public ResponseEntity<Page<OrganizationResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
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

    /**
     * Fetches a single organization by id.
     * Service layer validates the id and throws custom exceptions that are handled by
     * {@link com.cashclarity.exception.GlobalExceptionHandler}.
     */
    @GetMapping("/{organizationId}")
    public ResponseEntity<OrganizationResponse> getById(@PathVariable Long organizationId) {
        Organization organization = organizationService.getById(organizationId);
        return ResponseEntity.ok(OrganizationMapper.toResponse(organization));
    }

    /**
     * Updates mutable organization fields (name, email, timezone, currency, contact info, logo).
     * Request body is validated by Bean Validation; service layer validates business rules and
     * throws custom exceptions handled by {@link com.cashclarity.exception.GlobalExceptionHandler}.
     */
    @PatchMapping("/{organizationId}")
    public ResponseEntity<OrganizationResponse> update(
            @PathVariable Long organizationId,
            @Valid @RequestBody OrganizationUpdateRequest request
    ) {
        Organization updated = organizationService.update(organizationId, request);
        return ResponseEntity.ok(OrganizationMapper.toResponse(updated));
    }

    /**
     * Archives (soft-deletes) an organization by id.
     * Service layer validates the id and throws custom exceptions that are handled by
     * {@link com.cashclarity.exception.GlobalExceptionHandler}.
     */
    @DeleteMapping("/{organizationId}")
    public ResponseEntity<Void> delete(@PathVariable Long organizationId) {
        organizationService.delete(organizationId);
        return ResponseEntity.noContent().build();
    }
}


