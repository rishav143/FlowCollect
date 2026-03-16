package com.flowcollect.application.organization;

import java.util.UUID;

import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.organization.OrganizationStatus;
import com.flowcollect.exception.http.NotFoundException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.organization.OrganizationJpaRepository;

import java.util.Currency;
import java.time.ZoneId;
import java.time.LocalDate;


public class OrganizationUtil {
    public static Organization getOrganizationOrThrow
    (
        UUID organizationId, 
        OrganizationJpaRepository organizationRepository
    ) {
        if (organizationId == null) {
            throw new ValidationException(
                "Organization ID must not be null");
        }
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException(
                    "Organization not found with ID: " + organizationId));
        if (organization.isDeleted()) {
            throw new NotFoundException(
                "Organization with ID: " + organizationId + " is already archived.");
        }
        return organization;
    }

    public static void validateOrganizationIds(UUID organizationId1, UUID organizationId2) {
        if (organizationId1 == null || organizationId2 == null) {
            throw new ValidationException(
                "Organization IDs must not be null");
        }
        if (!organizationId1.equals(organizationId2)) {
            throw new ValidationException(
                "Organization IDs must be the same");
        }
    }

    public static ZoneId parseTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw new ValidationException(
                "Timezone must not be null or blank");
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (Exception e) {
            throw new ValidationException(
                "Unsupported timezone value '" + timezone + "' for organization");
        }
    }

    public static Currency parseCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new ValidationException(
                "Currency code must not be null or blank");
        }
        try {
            return Currency.getInstance(currencyCode.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(
                "Unsupported currency code '" + currencyCode + "' for organization");
        }
    }

    public static OrganizationStatus parseOrganizationStatus
    (
        String status
    ) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrganizationStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(
                "Unsupported status value '" + status + "' for organization");
        }
    }

    public static void validateDateRange
    (
        LocalDate createdFrom, 
        LocalDate createdTo
    ) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new ValidationException(
                "Created from must be before or equal to created to");
        }
    }
}
