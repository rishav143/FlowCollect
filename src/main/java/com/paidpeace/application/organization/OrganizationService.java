package com.paidpeace.application.organization;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paidpeace.api.v1.organization.dto.OrganizationCreateRequest;
import com.paidpeace.api.v1.organization.dto.OrganizationUpdateRequest;
import com.paidpeace.domain.organization.Organization;
import com.paidpeace.domain.organization.OrganizationStatus;
import com.paidpeace.exception.http.ConflictException;
import com.paidpeace.exception.http.ValidationException;
import com.paidpeace.infrastructure.persistence.organization.OrganizationJpaRepository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.util.Currency;
import com.paidpeace.domain.organization.OrganizationStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationJpaRepository organizationRepository;

    public OrganizationService(OrganizationJpaRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    // Create a new organization after validating timezone, currency, and email uniqueness.
    @Transactional
    public Organization create(OrganizationCreateRequest request) {
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

    // Get an organization by id.
    @Transactional(readOnly = true)
    public Organization getById(UUID organizationId) {
        return OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);
    }

    @Transactional(readOnly = true)
    public List<Organization> getEligibleOrganizationsForReminders(Collection<OrganizationStatus> statuses) {
        return organizationRepository.findAllByDeletedAtIsNullAndStatusIn(statuses);
    }

    // List organizations using filters and pagination.
    @Transactional(readOnly = true)
    public Page<Organization> list(
            String status,
            String email,
            String name,
            LocalDate createdFrom,
            LocalDate createdTo,
            Pageable pageable
    ) {
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

    // Hard-delete an organization by id.
    @Transactional
    public void delete(UUID organizationId) {
        Organization organization = OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);
        organizationRepository.delete(organization);
    }

    // Activate an organization by id.
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

    // Suspend an organization by id.
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

    // Archive (soft-delete) an organization by id.
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

    // Update an existing organization with the provided fields.
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
