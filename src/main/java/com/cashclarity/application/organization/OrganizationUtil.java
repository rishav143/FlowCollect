package com.cashclarity.application.organization;

import com.cashclarity.domain.organization.Organization;
import com.cashclarity.domain.organization.OrganizationStatus;
import com.cashclarity.exception.organization.InvalidOrganizationFieldException;
import com.cashclarity.exception.organization.OrganizationNotFoundException;
import com.cashclarity.infrastructure.persistence.organization.OrganizationJpaRepository;

import java.nio.file.Files;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.UUID;
import java.util.Locale;
import java.util.Currency;
import java.time.ZoneId;
import java.time.LocalDate;


public class OrganizationUtil {
    public static Organization getOrganizationOrThrow(UUID organizationId, OrganizationJpaRepository organizationRepository) {
        if (organizationId == null) {
            throw new InvalidOrganizationFieldException("organizationId with value " + organizationId + " must not be null");
        }
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException("organization not found with id " + organizationId));
        if (organization.isDeleted()) {
            throw new InvalidOrganizationFieldException("organization is deleted with id " + organizationId);
        }
        return organization;
    }

    public static ZoneId parseTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw new InvalidOrganizationFieldException("timezone must not be null or blank");
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (Exception e) {
            throw new InvalidOrganizationFieldException("unsupported timezone value '" + timezone + "'");
        }
    }

    public static Currency parseCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new InvalidOrganizationFieldException("currencyCode must not be null or blank");
        }
        try {
            return Currency.getInstance(currencyCode.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidOrganizationFieldException("unsupported currency code '" + currencyCode + "'");
        }
    }

    public static OrganizationStatus parseOrganizationStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrganizationStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidOrganizationFieldException("unsupported status value '" + status + "'");
        }
    }

    public static void validateDateRange(LocalDate createdFrom, LocalDate createdTo) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new InvalidOrganizationFieldException("createdFrom must be before or equal to createdTo");
        }
    }

    public static String buildLogoFilename(String originalName) {
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
