package com.paidpeace.application.organization;

import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.UUID;

import com.paidpeace.domain.organization.Organization;
import com.paidpeace.domain.organization.OrganizationStatus;
import com.paidpeace.exception.code.OrganizationErrorCode;
import com.paidpeace.exception.http.ConflictException;
import com.paidpeace.exception.http.NotFoundException;
import com.paidpeace.exception.http.ValidationException;
import com.paidpeace.infrastructure.persistence.organization.OrganizationJpaRepository;

import java.util.Locale;
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
            throw new ValidationException(OrganizationErrorCode.INVALID_ORGANIZATION_FIELD,
                "Organization ID must not be null");
        }
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND,
                    "Organization not found with ID: " + organizationId));
        if (organization.isDeleted()) {
            throw new ConflictException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND,
                "Organization with ID: " + organizationId + " is already archived.");
        }
        return organization;
    }

    public static void validateOrganizationIds(UUID organizationId1, UUID organizationId2) {
        if (organizationId1 == null || organizationId2 == null) {
            throw new ValidationException(OrganizationErrorCode.INVALID_ORGANIZATION_FIELD,
                "Organization IDs must not be null");
        }
        if (!organizationId1.equals(organizationId2)) {
            throw new ValidationException(OrganizationErrorCode.INVALID_ORGANIZATION_FIELD,
                "Organization IDs must be the same");
        }
    }

    public static ZoneId parseTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw new ValidationException(OrganizationErrorCode.INVALID_ORGANIZATION_FIELD,
                "Timezone must not be null or blank");
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (Exception e) {
            throw new ValidationException(OrganizationErrorCode.INVALID_ORGANIZATION_FIELD,
                "Unsupported timezone value '" + timezone + "' for organization");
        }
    }

    public static Currency parseCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new ValidationException(OrganizationErrorCode.INVALID_ORGANIZATION_FIELD,
                "Currency code must not be null or blank");
        }
        try {
            return Currency.getInstance(currencyCode.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException(OrganizationErrorCode.INVALID_ORGANIZATION_FIELD,
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
            throw new ValidationException(OrganizationErrorCode.INVALID_ORGANIZATION_FIELD,
                "Unsupported status value '" + status + "' for organization");
        }
    }

    public static void validateDateRange
    (
        LocalDate createdFrom, 
        LocalDate createdTo
    ) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new ValidationException(OrganizationErrorCode.INVALID_ORGANIZATION_FIELD,
                "Created from must be before or equal to created to");
        }
    }

    public static String buildLogoFilename
    (
        String originalName
    ) {
        if (originalName == null || originalName.isBlank()) {
            throw new ValidationException(OrganizationErrorCode.INVALID_ORGANIZATION_FIELD,
                "Original name must not be null or blank");
        }
        String extension = "";
        if (originalName != null) {
            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < originalName.length() - 1) {
                extension = originalName.substring(dotIndex).toLowerCase(Locale.ROOT);
            }
        }
        String base = "logo-" + UUID.randomUUID();
        return extension.isBlank() ? base : base + extension;
    }

    private static final String LOGO_UPLOAD_DIR = "uploads/organizations";

    public static void deleteLogoFileIfPresent(String logoUrl) {
        if (logoUrl == null || logoUrl.isBlank()) {
            return;
        }
        String normalized = logoUrl.startsWith("/") ? logoUrl.substring(1) : logoUrl;
        if (!normalized.startsWith(LOGO_UPLOAD_DIR)) {
            return;
        }
        Path path = Paths.get(normalized);
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Best-effort cleanup; persistence is handled by clearing logoUrl.
        }
    }
}
