package com.cashclarity.application.organization;

import com.cashclarity.api.v1.organization.dto.OrganizationCreateRequest;
import com.cashclarity.api.v1.organization.dto.OrganizationUpdateRequest;
import com.cashclarity.domain.organization.Organization;
import com.cashclarity.domain.organization.OrganizationStatus;
import com.cashclarity.exception.InvalidCurrencyException;
import com.cashclarity.exception.InvalidOrganizationFieldException;
import com.cashclarity.exception.InvalidOrganizationIdException;
import com.cashclarity.exception.InvalidTimezoneException;
import com.cashclarity.exception.OrganizationAlreadyArchivedException;
import com.cashclarity.exception.OrganizationAlreadyExistsException;
import com.cashclarity.exception.OrganizationNotFoundException;
import com.cashclarity.infrastructure.persistence.organization.OrganizationJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Set;

/**
 * Application service for organization use cases.
 * Validates business rules (timezone, currency, duplicate email) and delegates
 * persistence to the repository. Throws custom exceptions so the controller
 * layer can translate them into appropriate HTTP responses.
 */
@Service
public class OrganizationService {

    private final OrganizationJpaRepository organizationRepository;

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
        ZoneId timezone = parseTimezone(request.getTimezone());
        Currency currency = parseCurrency(request.getCurrency());

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
        validateOrganizationId(organizationId);
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
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
            Instant createdFrom,
            Instant createdTo,
            Pageable pageable
    ) {
        validatePageable(pageable);
        validateDateRange(createdFrom, createdTo);

        OrganizationStatus parsedStatus = parseStatus(status);

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
        if (createdFrom != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
        }
        if (createdTo != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), createdTo));
        }

        return organizationRepository.findAll(spec, pageable);
    }

    /**
     * Archives (soft-deletes) an organization by id.
     *
     * @param organizationId organization identifier
     * @throws InvalidOrganizationIdException if id is null or non-positive
     * @throws OrganizationNotFoundException if no organization exists for the given id
     * @throws OrganizationAlreadyArchivedException if the organization is already archived
     */
    @Transactional
    public void delete(Long organizationId) {
        validateOrganizationId(organizationId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        if (organization.isDeleted()) {
            throw new OrganizationAlreadyArchivedException(organizationId);
        }

        organization.archive();
        organizationRepository.save(organization);
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
        validateOrganizationId(organizationId);
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
            ZoneId timezone = parseTimezone(request.getTimezone());
            if (!timezone.equals(organization.getTimezone())) {
                organization.setTimezone(timezone);
                changed = true;
            }
        }

        if (request.getCurrency() != null) {
            Currency currency = parseCurrency(request.getCurrency());
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

    private ZoneId parseTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw new InvalidTimezoneException(timezone);
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (Exception e) {
            throw new InvalidTimezoneException(timezone, e);
        }
    }

    private Currency parseCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new InvalidCurrencyException(currencyCode);
        }
        try {
            return Currency.getInstance(currencyCode.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidCurrencyException(currencyCode, e);
        }
    }

    private void validateOrganizationId(Long organizationId) {
        if (organizationId == null || organizationId <= 0) {
            throw new InvalidOrganizationIdException(organizationId);
        }
    }

    private OrganizationStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrganizationStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidOrganizationFieldException("status", "unsupported value '" + status + "'");
        }
    }

    private void validateDateRange(Instant createdFrom, Instant createdTo) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new InvalidOrganizationFieldException("createdFrom", "must be before or equal to createdTo");
        }
    }

    private void validatePageable(Pageable pageable) {
        if (pageable == null) {
            throw new InvalidOrganizationFieldException("page", "pageable is required");
        }
        if (pageable.getPageNumber() < 0) {
            throw new InvalidOrganizationFieldException("page", "must be greater than or equal to 0");
        }
        int size = pageable.getPageSize();
        if (size <= 0 || size > 100) {
            throw new InvalidOrganizationFieldException("size", "must be between 1 and 100");
        }

        Set<String> allowedSort = Set.of(
                "id",
                "name",
                "email",
                "status",
                "createdAt",
                "updatedAt"
        );
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSort.contains(order.getProperty())) {
                throw new InvalidOrganizationFieldException("sort", "unsupported property '" + order.getProperty() + "'");
            }
        }
    }
}
