package com.cashclarity.api.v1.organization;

import com.cashclarity.api.v1.organization.dto.OrganizationCreateRequest;
import com.cashclarity.api.v1.organization.dto.OrganizationLogoResponse;
import com.cashclarity.api.v1.organization.dto.OrganizationSettingsRequest;
import com.cashclarity.api.v1.organization.dto.OrganizationSettingsResponse;
import com.cashclarity.api.v1.organization.dto.OrganizationUpdateRequest;
import com.cashclarity.api.v1.organization.dto.OrganizationResponse;
import com.cashclarity.api.v1.organization.dto.OrganizationStatusResponse;
import com.cashclarity.application.organization.OrganizationService;
import com.cashclarity.domain.organization.Organization;
import com.cashclarity.domain.organization.OrganizationStatus;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

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
     * currency, and duplicate email, throwing exceptions that are handled by {@link com.cashclarity.api.error.GlobalExceptionHandler}.
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
 * {@link com.cashclarity.api.error.GlobalExceptionHandler}.
     */
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

    /**
     * Fetches a single organization by id.
     * Service layer validates the id and throws custom exceptions that are handled by
     * {@link com.cashclarity.api.error.GlobalExceptionHandler}.
     */
    @GetMapping("/{organizationId}")
    public ResponseEntity<OrganizationResponse> getById(@PathVariable UUID organizationId) {
        Organization organization = organizationService.getById(organizationId);
        return ResponseEntity.ok(OrganizationMapper.toResponse(organization));
    }

    /**
     * Fetches the current status of an organization by id.
     * Service layer validates the id and throws custom exceptions that are handled by
     * {@link com.cashclarity.api.error.GlobalExceptionHandler}.
     */
    @GetMapping("/{organizationId}/status")
    public ResponseEntity<OrganizationStatusResponse> getStatus(@PathVariable UUID organizationId) {
        OrganizationStatus status = organizationService.getStatus(organizationId);
        return ResponseEntity.ok(new OrganizationStatusResponse(organizationId, status));
    }

    /**
     * Uploads a logo for an organization.
     * Service layer validates the id and throws custom exceptions that are handled by
     * {@link com.cashclarity.api.error.GlobalExceptionHandler}.
     */
    @PostMapping("/{organizationId}/logo")
    public ResponseEntity<OrganizationLogoResponse> uploadLogo(
            @PathVariable UUID organizationId,
            @RequestParam("file") MultipartFile file
    ) {
        Organization updated = organizationService.uploadLogo(organizationId, file);
        return ResponseEntity.ok(new OrganizationLogoResponse(updated.getId(), updated.getLogoUrl()));
    }

    /**
     * Removes an organization's logo.
     * Service layer validates the id and throws custom exceptions that are handled by
     * {@link com.cashclarity.api.error.GlobalExceptionHandler}.
     */
    @DeleteMapping("/{organizationId}/logo")
    public ResponseEntity<Void> removeLogo(@PathVariable UUID organizationId) {
        organizationService.removeLogo(organizationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Fetches the logo URL for an organization.
     * Service layer validates the id and throws custom exceptions that are handled by
     * {@link com.cashclarity.api.error.GlobalExceptionHandler}.
     */
    @GetMapping("/{organizationId}/logo")
    public ResponseEntity<OrganizationLogoResponse> getLogo(@PathVariable UUID organizationId) {
        String logoUrl = organizationService.getLogoUrl(organizationId);
        return ResponseEntity.ok(new OrganizationLogoResponse(organizationId, logoUrl));
    }

    /**
     * Fetches organization settings (timezone, currency).
     * Service layer validates the id and throws custom exceptions that are handled by
     * {@link com.cashclarity.api.error.GlobalExceptionHandler}.
     */
    @GetMapping("/{organizationId}/settings")
    public ResponseEntity<OrganizationSettingsResponse> getSettings(@PathVariable UUID organizationId) {
        Organization organization = organizationService.getSettings(organizationId);
        return ResponseEntity.ok(new OrganizationSettingsResponse(
                organization.getId(),
                organization.getTimezone() != null ? organization.getTimezone().getId() : null,
                organization.getCurrency() != null ? organization.getCurrency().getCurrencyCode() : null
        ));
    }

    /**
     * Updates organization settings (timezone, currency).
     * Service layer validates the id and throws custom exceptions that are handled by
     * {@link com.cashclarity.api.error.GlobalExceptionHandler}.
     */
    @PatchMapping("/{organizationId}/settings")
    public ResponseEntity<OrganizationSettingsResponse> updateSettings(
            @PathVariable UUID organizationId,
            @RequestBody OrganizationSettingsRequest request
    ) {
        Organization updated = organizationService.updateSettings(organizationId, request);
        return ResponseEntity.ok(new OrganizationSettingsResponse(
                updated.getId(),
                updated.getTimezone() != null ? updated.getTimezone().getId() : null,
                updated.getCurrency() != null ? updated.getCurrency().getCurrencyCode() : null
        ));
    }

    /**
     * Updates mutable organization fields (name, email, timezone, currency, contact info, logo).
     * Request body is validated by Bean Validation; service layer validates business rules and
     * throws custom exceptions handled by {@link com.cashclarity.api.error.GlobalExceptionHandler}.
     */
    @PatchMapping("/{organizationId}")
    public ResponseEntity<OrganizationResponse> update(
            @PathVariable UUID organizationId,
            @Valid @RequestBody OrganizationUpdateRequest request
    ) {
        Organization updated = organizationService.update(organizationId, request);
        return ResponseEntity.ok(OrganizationMapper.toResponse(updated));
    }

    /**
     * Hard-deletes an organization by id.
     * Service layer validates the id and throws custom exceptions that are handled by
     * {@link com.cashclarity.api.error.GlobalExceptionHandler}.
     */
    @DeleteMapping("/{organizationId}")
    public ResponseEntity<Void> delete(@PathVariable UUID organizationId) {
        organizationService.delete(organizationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activates an organization by id.
     * Service layer validates the id and throws custom exceptions that are handled by
     * {@link com.cashclarity.api.error.GlobalExceptionHandler}.
     */
    @PostMapping("/{organizationId}/activate")
    public ResponseEntity<OrganizationResponse> activate(@PathVariable UUID organizationId) {
        Organization activated = organizationService.activate(organizationId);
        return ResponseEntity.ok(OrganizationMapper.toResponse(activated));
    }

    /**
     * Suspends an organization by id.
     * Service layer validates the id and throws custom exceptions that are handled by
     * {@link com.cashclarity.api.error.GlobalExceptionHandler}.
     */
    @PostMapping("/{organizationId}/suspend")
    public ResponseEntity<OrganizationResponse> suspend(@PathVariable UUID organizationId) {
        Organization suspended = organizationService.suspend(organizationId);
        return ResponseEntity.ok(OrganizationMapper.toResponse(suspended));
    }

    /**
     * Archives an organization by id.
     * Service layer validates the id and throws custom exceptions that are handled by
     * {@link com.cashclarity.api.error.GlobalExceptionHandler}.
     */
    @PostMapping("/{organizationId}/archive")
    public ResponseEntity<OrganizationResponse> archive(@PathVariable UUID organizationId) {
        Organization archived = organizationService.archive(organizationId);
        return ResponseEntity.ok(OrganizationMapper.toResponse(archived));
    }

    /**
     * Moves an organization to trial status by id.
     * Service layer validates the id and throws custom exceptions that are handled by
     * {@link com.cashclarity.api.error.GlobalExceptionHandler}.
     */
    @PostMapping("/{organizationId}/trial")
    public ResponseEntity<OrganizationResponse> trial(@PathVariable UUID organizationId) {
        Organization trial = organizationService.trial(organizationId);
        return ResponseEntity.ok(OrganizationMapper.toResponse(trial));
    }

    /**
     * Marks an organization as expired by id.
     * Service layer validates the id and throws custom exceptions that are handled by
     * {@link com.cashclarity.api.error.GlobalExceptionHandler}.
     */
    @PostMapping("/{organizationId}/expired")
    public ResponseEntity<OrganizationResponse> expired(@PathVariable UUID organizationId) {
        Organization expired = organizationService.expired(organizationId);
        return ResponseEntity.ok(OrganizationMapper.toResponse(expired));
    }
}


