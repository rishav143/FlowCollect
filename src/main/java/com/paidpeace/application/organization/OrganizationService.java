package com.paidpeace.application.organization;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.paidpeace.api.v1.organization.dto.OrganizationCreateRequest;
import com.paidpeace.api.v1.organization.dto.OrganizationSettingsRequest;
import com.paidpeace.api.v1.organization.dto.OrganizationUpdateRequest;
import com.paidpeace.common.PaginationUtils;
import com.paidpeace.domain.organization.Organization;
import com.paidpeace.domain.organization.OrganizationStatus;
import com.paidpeace.exception.http.ConflictException;
import com.paidpeace.exception.http.NotFoundException;
import com.paidpeace.exception.http.ServiceUnavailableException;
import com.paidpeace.exception.http.ValidationException;
import com.paidpeace.infrastructure.persistence.organization.OrganizationJpaRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.UUID;

/**
 * Application service for organization use cases.
 * Validates business rules (timezone, currency, duplicate email) and delegates
 * persistence to the repository. Throws custom exceptions so the controller
 * layer can translate them into appropriate HTTP responses.
 */
@Service
public class OrganizationService {

    private final OrganizationJpaRepository organizationRepository;
    private static final String LOGO_UPLOAD_DIR = "uploads/organizations";

    public OrganizationService(OrganizationJpaRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    /**
     * Creates a new organization after validating timezone, currency, and email uniqueness.
     *
     * @param request validated create request (controller ensures @Valid)
     * @return the persisted organization
     */
    @Transactional
    public Organization create
    (
        OrganizationCreateRequest request
    ) {
        if (request == null) {
            throw new ValidationException(
                "Request must not be null");
        }


        if(request.getTimezone() == null || request.getTimezone().isBlank()) {
            throw new ValidationException(
                "Timezone is required. Provide a valid timezone ID (e.g. America/New_York, Europe/London).");
        }
        if(request.getCurrency() == null || request.getCurrency().isBlank()) {
            throw new ValidationException(
                "Currency is required. Provide a valid ISO 4217 code (e.g. USD, EUR, INR).");
        }
        ZoneId timezone = OrganizationUtil.parseTimezone(request.getTimezone());
        Currency currency = OrganizationUtil.parseCurrency(request.getCurrency());

        if (request.getName().trim().isBlank()) {
            throw new ValidationException(
                "Name must not be blank");
        }
        if (request.getEmail().trim().isBlank()) {
            throw new ValidationException(
                "Email must not be blank");
        }
        if (organizationRepository.existsByEmail(request.getEmail().trim())) {
            throw new ValidationException(
                "Organization with email: " + request.getEmail() + " already exists.");
        }

        Organization organization = new Organization(
                request.getName().trim(),
                request.getEmail().trim(),
                timezone,
                currency
        );
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            organization.setPhone(request.getPhone().trim());
        }
        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            organization.setAddress(request.getAddress().trim());
        }

        return organizationRepository.save(organization);
    }

    /**
     * Fetches an organization by id.
     *
     * @param organizationId organization identifier
     * @return the organization
     */
    @Transactional(readOnly = true)
    public Organization getById(UUID organizationId) {
        return OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);
    }

    /**
     * Fetches an organization's status by id.
     *
     * @param organizationId organization identifier
     * @return current organization status
     */
    @Transactional(readOnly = true)
    public OrganizationStatus getStatus(UUID organizationId) {
        return OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository).getStatus();
    }

    /**
     * Fetches organization settings (timezone, currency) by id.
     *
     * @param organizationId organization identifier
     * @return organization with current settings
     */
    @Transactional(readOnly = true)
    public Organization getSettings(UUID organizationId) {
        return OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);
    }

    /**
     * Updates organization settings (timezone, currency).
     *
     * @param organizationId organization identifier
     * @param request settings payload
     * @return updated organization
     */
    @Transactional
    public Organization updateSettings(UUID organizationId, OrganizationSettingsRequest request) {
        Organization organization = OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);

        if (organization.isDeleted()) {
            throw new ValidationException(
                "Organization with ID: " + organizationId + " is already archived.");
        }

        if (request == null) {
            throw new ValidationException(
                "Settings must not be null");
        }

        boolean hasTimezone = request.getTimezone() != null;
        boolean hasCurrency = request.getCurrency() != null;
        if (!hasTimezone && !hasCurrency) {
            throw new ValidationException("Settings must contain timezone and currency");
        }

        boolean changed = false;
        if (hasTimezone) {
            ZoneId timezone = OrganizationUtil.parseTimezone(request.getTimezone());
            if (!timezone.equals(organization.getTimezone())) {
                organization.setTimezone(timezone);
                changed = true;
            }
        }

        if (hasCurrency) {
            Currency currency = OrganizationUtil.parseCurrency(request.getCurrency());
            if (!currency.equals(organization.getCurrency())) {
                organization.setCurrency(currency);
                changed = true;
            }
        }

        if (!changed) {
            return organization;
        }

        return organizationRepository.save(organization);
    }

    /**
     * Uploads a logo file and updates the organization's logoUrl.
     *
     * @param organizationId organization identifier
     * @param file logo file (multipart)
     * @return updated organization
     */
    @Transactional
    public Organization uploadLogo
    (
            UUID organizationId,
            MultipartFile file
    ) {
        Organization organization = OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);

        if (organization.isDeleted()) {
            throw new ValidationException(
                "Organization with ID: " + organizationId + " is already archived.");
        }

        if (file == null || file.isEmpty()) {
            throw new ValidationException(
                "Logo file must not be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new ValidationException(
                "Logo file must be an image");
        }

        String filename = OrganizationUtil.buildLogoFilename(file.getOriginalFilename());
        Path directory = Paths.get(LOGO_UPLOAD_DIR, String.valueOf(organizationId));
        Path target = directory.resolve(filename);
        try {
            Files.createDirectories(directory);
            file.transferTo(target);
        } catch (IOException ex) {
            throw new ServiceUnavailableException(
                "Failed to store logo file for organization with ID: " + organizationId + " due to: " + ex.getMessage());
        }

        String logoUrl = "/" + directory.resolve(filename).toString().replace("\\", "/");
        organization.setLogoUrl(logoUrl);
        return organizationRepository.save(organization);
    }

    /**
     * Removes an organization's logo.
     * @param organizationId organization identifier
     * @return the updated organization
     */
    @Transactional
    public void removeLogo(UUID organizationId) {
        Organization organization = OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);

        if (organization.isDeleted()) {
            throw new ValidationException(
                "Organization with ID: " + organizationId + " is already archived.");
        }

        String logoUrl = organization.getLogoUrl();
        if (logoUrl == null || logoUrl.isBlank()) {
            throw new NotFoundException(
                "Organization with ID: " + organizationId + " has no logo.");
        }

        OrganizationUtil.deleteLogoFileIfPresent(logoUrl);
        organization.setLogoUrl(null);
        organizationRepository.save(organization);
    }

    /**
     * Retrieves the stored logo URL.
     *
     * @param organizationId organization identifier
     * @return logo URL
     */
    @Transactional(readOnly = true)
    public String getLogoUrl(UUID organizationId) {
        Organization organization = OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);

        String logoUrl = organization.getLogoUrl();
        if (logoUrl == null || logoUrl.isBlank()) {
            throw new NotFoundException(
                "Organization with ID: " + organizationId + " has no logo.");
        }
        return logoUrl;
    }

    /**
     * Lists organizations using filters and pagination.
     */
    @Transactional(readOnly = true)
    public Page<Organization> list(
            String status,
            String email,
            String name,
            LocalDate createdFrom,
            LocalDate createdTo,
            Pageable pageable
    ) {
        try {
            PaginationUtils.validatePageable(pageable);
        } catch (ValidationException e) {
            // Map user-level pageable validation errors to organization-level field errors
            String message = e.getMessage();
            String field = "page";
            String reason = message;
            try {
                int start = message.indexOf('\'');
                int end = message.indexOf('\'', start + 1);
                if (start >= 0 && end > start) {
                    field = message.substring(start + 1, end);
                }
                int colon = message.indexOf(':');
                if (colon >= 0 && colon + 1 < message.length()) {
                    reason = message.substring(colon + 1).trim();
                }
            } catch (Exception ignore) {
                // fallback to full message
                reason = message;
            }
            throw new ValidationException(
                field + " " + reason + " for organization list");
        }
        OrganizationUtil.validateDateRange(createdFrom, createdTo);

        OrganizationStatus parsedStatus = OrganizationUtil.parseOrganizationStatus(status);

        Instant createdFromInstant = createdFrom != null ? createdFrom.atStartOfDay(ZoneOffset.UTC).toInstant() : null;
        Instant createdToInstant = createdTo != null ? createdTo.plusDays(1).atStartOfDay(ZoneOffset.UTC).minusNanos(1).toInstant() : null;

        Specification<Organization> spec = (root, query, cb) -> cb.conjunction();
        if (parsedStatus != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), parsedStatus));
        }
        if (email != null && !email.isBlank()) {
            String emailLike = "%" + email.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("email")), emailLike));
        }
        if (name != null && !name.isBlank()) {
            String nameLike = "%" + name.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), nameLike));
        }
        if (createdFromInstant != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), createdFromInstant));
        }
        if (createdToInstant != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), createdToInstant));
        }

        return organizationRepository.findAll(spec, pageable);
    }

    /**
     * Hard-deletes an organization by id.
     *
     * @param organizationId organization identifier
     * @throws InvalidOrganizationIdException if id is null or non-positive
     * @throws OrganizationNotFoundException if no organization exists for the given id
     */
    @Transactional
    public void delete(UUID organizationId) {
        Organization organization = OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);
        organizationRepository.delete(organization);
    }

    /**
     * Activates an organization by id.
     *
     * @param organizationId organization identifier
     * @return the updated organization
     */
    @Transactional
    public Organization activate(UUID organizationId) {
        Organization organization = OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);

        if (organization.isDeleted()) {
            throw new ConflictException(
                "Organization with ID: " + organizationId + " is already archived.");
        }

        if (organization.getStatus() == OrganizationStatus.ACTIVE) {
            throw new ConflictException(
                "Organization with ID: " + organizationId + " is already active.");
        }

        organization.activate();
        return organizationRepository.save(organization);
    }

    /**
     * Suspends an organization by id.
     *
     * @param organizationId organization identifier
     * @return the updated organization
     */
    @Transactional
    public Organization suspend(UUID organizationId) {
        Organization organization = OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);

        if (organization.isDeleted()) {
            throw new ConflictException(
                "Organization with ID: " + organizationId + " is already archived.");
        }

        if (organization.getStatus() == OrganizationStatus.SUSPENDED) {
            throw new ConflictException(
                "Organization with ID: " + organizationId + " is already suspended.");
        }

        organization.suspend();
        return organizationRepository.save(organization);
    }

    /**
     * Archives (soft-deletes) an organization by id.
     *
     * @param organizationId organization identifier
     * @return the updated organization
     */
    @Transactional
    public Organization archive(UUID organizationId) {
        Organization organization = OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);

        if (organization.isDeleted()) {
            throw new ConflictException(
                "Organization with ID: " + organizationId + " is already archived.");
        }

        organization.archive();
        return organizationRepository.save(organization);
    }

    /**
     * Puts an organization into trial status by id.
     *
     * @param organizationId organization identifier
     * @return the updated organization
     */
    @Transactional
    public Organization trial(UUID organizationId) {
        Organization organization = OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);

        if (organization.isDeleted()) {
            throw new ConflictException(
                "Organization with ID: " + organizationId + " is already archived.");
        }

        if (organization.getStatus() == OrganizationStatus.TRIAL) {
            throw new ConflictException(
                "Organization with ID: " + organizationId + " is already in trial.");
        }

        organization.trial();
        return organizationRepository.save(organization);
    }

    /**
     * Marks an organization as expired by id.
     *
     * @param organizationId organization identifier
     * @return the updated organization
     */
    @Transactional
    public Organization expired(UUID organizationId) {
        Organization organization = OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);

        if (organization.isDeleted()) {
            throw new ConflictException(
                "Organization with ID: " + organizationId + " is already archived.");
        }

        if (organization.getStatus() == OrganizationStatus.EXPIRED) {
            throw new ConflictException(
                "Organization with ID: " + organizationId + " is already expired.");
        }

        organization.expired();
        return organizationRepository.save(organization);
    }

    /**
     * Updates an existing organization with the provided fields.
     *
     * @param organizationId organization identifier
     * @param request update payload (nullable fields are ignored)
     * @return the updated organization
     */
    @Transactional
    public Organization update(UUID organizationId, OrganizationUpdateRequest request) {
        Organization organization = OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);

        boolean changed = false;

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isBlank()) {
                throw new ValidationException(
                    "Name must not be blank");
            }
            if (!name.equals(organization.getName())) {
                organization.setName(name);
                changed = true;
            }
        }

        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (email.isBlank()) {
                throw new ValidationException(
                    "Email must not be blank");
            }
            if (!email.equalsIgnoreCase(organization.getEmail())
                    && organizationRepository.existsByEmailAndIdNot(email, organizationId)) {
                throw new ConflictException(
                    "Organization with email: " + email + " already exists.");
            }
            if (!email.equals(organization.getEmail())) {
                organization.setEmail(email);
                changed = true;
            }
        }

        if (request.getTimezone() != null) {
            ZoneId timezone = OrganizationUtil.parseTimezone(request.getTimezone());
            if (!timezone.equals(organization.getTimezone())) {
                organization.setTimezone(timezone);
                changed = true;
            }
        }

        if (request.getCurrency() != null) {
            Currency currency = OrganizationUtil.parseCurrency(request.getCurrency());
            if (!currency.equals(organization.getCurrency())) {
                organization.setCurrency(currency);
                changed = true;
            }
        }

        if (request.getPhone() != null) {
            String phone = request.getPhone().trim();
            String normalizedPhone = phone.isBlank() ? null : phone;
            if (normalizedPhone == null ? organization.getPhone() != null : !normalizedPhone.equals(organization.getPhone())) {
                organization.setPhone(normalizedPhone);
                changed = true;
            }
        }

        if (request.getAddress() != null) {
            String address = request.getAddress().trim();
            String normalizedAddress = address.isBlank() ? null : address;
            if (normalizedAddress == null ? organization.getAddress() != null : !normalizedAddress.equals(organization.getAddress())) {
                organization.setAddress(normalizedAddress);
                changed = true;
            }
        }

        if (request.getLogoUrl() != null) {
            String logoUrl = request.getLogoUrl().trim();
            String normalizedLogoUrl = logoUrl.isBlank() ? null : logoUrl;
            if (normalizedLogoUrl == null ? organization.getLogoUrl() != null : !normalizedLogoUrl.equals(organization.getLogoUrl())) {
                organization.setLogoUrl(normalizedLogoUrl);
                changed = true;
            }
        }

        if (!changed) {
            return organization;
        }

        return organizationRepository.save(organization);
    }
}
