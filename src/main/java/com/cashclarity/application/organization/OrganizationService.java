package com.cashclarity.application.organization;

import com.cashclarity.api.v1.organization.dto.OrganizationCreateRequest;
import com.cashclarity.api.v1.organization.dto.OrganizationSettingsRequest;
import com.cashclarity.api.v1.organization.dto.OrganizationUpdateRequest;
import com.cashclarity.application.util;
import com.cashclarity.domain.organization.Organization;
import com.cashclarity.domain.organization.OrganizationStatus;
import com.cashclarity.exception.organization.InvalidCurrencyException;
import com.cashclarity.exception.organization.InvalidOrganizationFieldException;
import com.cashclarity.exception.organization.InvalidOrganizationIdException;
import com.cashclarity.exception.organization.InvalidTimezoneException;
import com.cashclarity.exception.organization.OrganizationAlreadyActiveException;
import com.cashclarity.exception.organization.OrganizationAlreadyArchivedException;
import com.cashclarity.exception.organization.OrganizationAlreadyExpiredException;
import com.cashclarity.exception.organization.OrganizationAlreadyInTrialException;
import com.cashclarity.exception.organization.OrganizationAlreadySuspendedException;
import com.cashclarity.exception.organization.OrganizationAlreadyExistsException;
import com.cashclarity.exception.organization.OrganizationLogoNotFoundException;
import com.cashclarity.exception.organization.OrganizationNotFoundException;
import com.cashclarity.infrastructure.persistence.organization.OrganizationJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
     * @throws InvalidTimezoneException  if timezone string is not a valid ZoneId
     * @throws InvalidCurrencyException  if currency code is not a valid ISO 4217 code
     * @throws OrganizationAlreadyExistsException if an organization with the same email already exists
     */
    @Transactional
    public Organization create(OrganizationCreateRequest request) {
        ZoneId timezone = util.parseTimezone(request.getTimezone());
        Currency currency = util.parseCurrency(request.getCurrency());

        if (organizationRepository.existsByEmail(request.getEmail().trim())) {
            throw new OrganizationAlreadyExistsException(request.getEmail());
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
     * @throws InvalidOrganizationIdException if id is null or non-positive
     * @throws OrganizationNotFoundException if no organization exists for the given id
     */
    @Transactional(readOnly = true)
    public Organization getById(Long organizationId) {
        util.validateOrganizationId(organizationId);
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }

    /**
     * Fetches an organization's status by id.
     *
     * @param organizationId organization identifier
     * @return current organization status
     * @throws InvalidOrganizationIdException if id is null or non-positive
     * @throws OrganizationNotFoundException if no organization exists for the given id
     */
    @Transactional(readOnly = true)
    public OrganizationStatus getStatus(Long organizationId) {
        util.validateOrganizationId(organizationId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        return organization.getStatus();
    }

    /**
     * Fetches organization settings (timezone, currency) by id.
     *
     * @param organizationId organization identifier
     * @return organization with current settings
     * @throws InvalidOrganizationIdException if id is null or non-positive
     * @throws OrganizationNotFoundException if no organization exists for the given id
     */
    @Transactional(readOnly = true)
    public Organization getSettings(Long organizationId) {
        util.validateOrganizationId(organizationId);
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
    }

    /**
     * Updates organization settings (timezone, currency).
     *
     * @param organizationId organization identifier
     * @param request settings payload
     * @return updated organization
     * @throws InvalidOrganizationIdException if id is null or non-positive
     * @throws OrganizationNotFoundException if no organization exists for the given id
     * @throws OrganizationAlreadyArchivedException if the organization is archived
     * @throws InvalidOrganizationFieldException if no settings are provided
     * @throws InvalidTimezoneException if timezone is invalid
     * @throws InvalidCurrencyException if currency is invalid
     */
    @Transactional
    public Organization updateSettings(Long organizationId, OrganizationSettingsRequest request) {
        util.validateOrganizationId(organizationId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        if (organization.isDeleted()) {
            throw new OrganizationAlreadyArchivedException(organizationId);
        }

        if (request == null) {
            throw new InvalidOrganizationFieldException("settings", "timezone or currency must be provided");
        }

        boolean hasTimezone = request.getTimezone() != null;
        boolean hasCurrency = request.getCurrency() != null;
        if (!hasTimezone && !hasCurrency) {
            throw new InvalidOrganizationFieldException("settings", "timezone or currency must be provided");
        }

        boolean changed = false;
        if (hasTimezone) {
            ZoneId timezone = util.parseTimezone(request.getTimezone());
            if (!timezone.equals(organization.getTimezone())) {
                organization.setTimezone(timezone);
                changed = true;
            }
        }

        if (hasCurrency) {
            Currency currency = util.parseCurrency(request.getCurrency());
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
     * @throws InvalidOrganizationIdException if id is null or non-positive
     * @throws OrganizationNotFoundException if no organization exists for the given id
     * @throws OrganizationAlreadyArchivedException if the organization is archived
     * @throws InvalidOrganizationFieldException if the logo file is invalid
     */
    @Transactional
    public Organization uploadLogo(Long organizationId, MultipartFile file) {
        util.validateOrganizationId(organizationId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        if (organization.isDeleted()) {
            throw new OrganizationAlreadyArchivedException(organizationId);
        }

        if (file == null || file.isEmpty()) {
            throw new InvalidOrganizationFieldException("logo", "file must not be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new InvalidOrganizationFieldException("logo", "unsupported file type");
        }

        String filename = util.buildLogoFilename(file.getOriginalFilename());
        Path directory = Paths.get(LOGO_UPLOAD_DIR, String.valueOf(organizationId));
        Path target = directory.resolve(filename);
        try {
            Files.createDirectories(directory);
            file.transferTo(target);
        } catch (IOException ex) {
            throw new InvalidOrganizationFieldException("logo", "failed to store file");
        }

        String logoUrl = "/" + directory.resolve(filename).toString().replace("\\", "/");
        organization.setLogoUrl(logoUrl);
        return organizationRepository.save(organization);
    }

    /**
     * Removes an organization's logo.
     *
     * @param organizationId organization identifier
     * @throws InvalidOrganizationIdException if id is null or non-positive
     * @throws OrganizationNotFoundException if no organization exists for the given id
     * @throws OrganizationAlreadyArchivedException if the organization is archived
     * @throws OrganizationLogoNotFoundException if no logo is set
     */
    @Transactional
    public void removeLogo(Long organizationId) {
        util.validateOrganizationId(organizationId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        if (organization.isDeleted()) {
            throw new OrganizationAlreadyArchivedException(organizationId);
        }

        String logoUrl = organization.getLogoUrl();
        if (logoUrl == null || logoUrl.isBlank()) {
            throw new OrganizationLogoNotFoundException(organizationId);
        }

        util.deleteLogoFileIfPresent(logoUrl);
        organization.setLogoUrl(null);
        organizationRepository.save(organization);
    }

    /**
     * Retrieves the stored logo URL.
     *
     * @param organizationId organization identifier
     * @return logo URL
     * @throws InvalidOrganizationIdException if id is null or non-positive
     * @throws OrganizationNotFoundException if no organization exists for the given id
     * @throws OrganizationLogoNotFoundException if no logo is set
     */
    @Transactional(readOnly = true)
    public String getLogoUrl(Long organizationId) {
        util.validateOrganizationId(organizationId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        String logoUrl = organization.getLogoUrl();
        if (logoUrl == null || logoUrl.isBlank()) {
            throw new OrganizationLogoNotFoundException(organizationId);
        }
        return logoUrl;
    }

    /**
     * Lists organizations using filters and pagination.
     *
     * @param status status filter (optional)
     * @param email email filter (optional, substring match)
     * @param name name filter (optional, substring match)
     * @param createdFrom createdAt lower bound (optional)
     * @param createdTo createdAt upper bound (optional)
     * @param pageable pagination and sorting
     * @return page of organizations
     * @throws InvalidOrganizationFieldException if filters are invalid
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
        util.validatePageable(pageable);
        util.validateDateRange(createdFrom, createdTo);

        OrganizationStatus parsedStatus = util.parseOrganizationStatus(status);

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
    public void delete(Long organizationId) {
        util.validateOrganizationId(organizationId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        organizationRepository.delete(organization);
    }

    /**
     * Activates an organization by id.
     *
     * @param organizationId organization identifier
     * @return the updated organization
     * @throws InvalidOrganizationIdException if id is null or non-positive
     * @throws OrganizationNotFoundException if no organization exists for the given id
     * @throws OrganizationAlreadyArchivedException if the organization is archived
     * @throws OrganizationAlreadyActiveException if the organization is already active
     */
    @Transactional
    public Organization activate(Long organizationId) {
        util.validateOrganizationId(organizationId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        if (organization.isDeleted()) {
            throw new OrganizationAlreadyArchivedException(organizationId);
        }

        if (organization.getStatus() == OrganizationStatus.ACTIVE) {
            throw new OrganizationAlreadyActiveException(organizationId);
        }

        organization.activate();
        return organizationRepository.save(organization);
    }

    /**
     * Suspends an organization by id.
     *
     * @param organizationId organization identifier
     * @return the updated organization
     * @throws InvalidOrganizationIdException if id is null or non-positive
     * @throws OrganizationNotFoundException if no organization exists for the given id
     * @throws OrganizationAlreadyArchivedException if the organization is archived
     * @throws OrganizationAlreadySuspendedException if the organization is already suspended
     */
    @Transactional
    public Organization suspend(Long organizationId) {
        util.validateOrganizationId(organizationId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        if (organization.isDeleted()) {
            throw new OrganizationAlreadyArchivedException(organizationId);
        }

        if (organization.getStatus() == OrganizationStatus.SUSPENDED) {
            throw new OrganizationAlreadySuspendedException(organizationId);
        }

        organization.suspend();
        return organizationRepository.save(organization);
    }

    /**
     * Archives (soft-deletes) an organization by id.
     *
     * @param organizationId organization identifier
     * @return the updated organization
     * @throws InvalidOrganizationIdException if id is null or non-positive
     * @throws OrganizationNotFoundException if no organization exists for the given id
     * @throws OrganizationAlreadyArchivedException if the organization is already archived
     */
    @Transactional
    public Organization archive(Long organizationId) {
        util.validateOrganizationId(organizationId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        if (organization.isDeleted()) {
            throw new OrganizationAlreadyArchivedException(organizationId);
        }

        organization.archive();
        return organizationRepository.save(organization);
    }

    /**
     * Puts an organization into trial status by id.
     *
     * @param organizationId organization identifier
     * @return the updated organization
     * @throws InvalidOrganizationIdException if id is null or non-positive
     * @throws OrganizationNotFoundException if no organization exists for the given id
     * @throws OrganizationAlreadyArchivedException if the organization is archived
     * @throws OrganizationAlreadyInTrialException if the organization is already in trial
     */
    @Transactional
    public Organization trial(Long organizationId) {
        util.validateOrganizationId(organizationId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        if (organization.isDeleted()) {
            throw new OrganizationAlreadyArchivedException(organizationId);
        }

        if (organization.getStatus() == OrganizationStatus.TRIAL) {
            throw new OrganizationAlreadyInTrialException(organizationId);
        }

        organization.setStatus(OrganizationStatus.TRIAL);
        return organizationRepository.save(organization);
    }

    /**
     * Marks an organization as expired by id.
     *
     * @param organizationId organization identifier
     * @return the updated organization
     * @throws InvalidOrganizationIdException if id is null or non-positive
     * @throws OrganizationNotFoundException if no organization exists for the given id
     * @throws OrganizationAlreadyArchivedException if the organization is archived
     * @throws OrganizationAlreadyExpiredException if the organization is already expired
     */
    @Transactional
    public Organization expired(Long organizationId) {
        util.validateOrganizationId(organizationId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        if (organization.isDeleted()) {
            throw new OrganizationAlreadyArchivedException(organizationId);
        }

        if (organization.getStatus() == OrganizationStatus.EXPIRED) {
            throw new OrganizationAlreadyExpiredException(organizationId);
        }

        organization.setStatus(OrganizationStatus.EXPIRED);
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
    public Organization update(Long organizationId, OrganizationUpdateRequest request) {
        util.validateOrganizationId(organizationId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        boolean changed = false;

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isBlank()) {
                throw new InvalidOrganizationFieldException("name", "must not be blank");
            }
            if (!name.equals(organization.getName())) {
                organization.setName(name);
                changed = true;
            }
        }

        if (request.getEmail() != null) {
            String email = request.getEmail().trim();
            if (email.isBlank()) {
                throw new InvalidOrganizationFieldException("email", "must not be blank");
            }
            if (!email.equalsIgnoreCase(organization.getEmail())
                    && organizationRepository.existsByEmailAndIdNot(email, organizationId)) {
                throw new OrganizationAlreadyExistsException(email);
            }
            if (!email.equals(organization.getEmail())) {
                organization.setEmail(email);
                changed = true;
            }
        }

        if (request.getTimezone() != null) {
            ZoneId timezone = util.parseTimezone(request.getTimezone());
            if (!timezone.equals(organization.getTimezone())) {
                organization.setTimezone(timezone);
                changed = true;
            }
        }

        if (request.getCurrency() != null) {
            Currency currency = util.parseCurrency(request.getCurrency());
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
