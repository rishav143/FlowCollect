package com.cashclarity.application;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;

import com.cashclarity.api.v1.invoice.dto.InvoiceItemRequest;
import com.cashclarity.domain.invoice.Invoice;
import com.cashclarity.domain.organization.Organization;
import com.cashclarity.domain.organization.OrganizationStatus;
import com.cashclarity.domain.user.UserRole;
import com.cashclarity.exception.organization.InvalidCurrencyException;
import com.cashclarity.exception.organization.InvalidTimezoneException;
import java.time.ZoneId;
import java.util.Currency;
import java.util.List;
import java.time.LocalDate;
import java.util.Locale;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.cashclarity.domain.user.UserStatus;
import com.cashclarity.exception.organization.InvalidOrganizationFieldException;
import com.cashclarity.exception.organization.InvalidOrganizationIdException;
import com.cashclarity.exception.organization.OrganizationAlreadyArchivedException;
import com.cashclarity.exception.organization.OrganizationNotFoundException;
import com.cashclarity.exception.user.InvalidUserFieldException;
import com.cashclarity.exception.user.InvalidUserIdException;
import com.cashclarity.infrastructure.persistence.organization.OrganizationJpaRepository;

public class util {
    
    // Organization related methods
    public static Organization getOrganizationOrThrow(Long organizationId, OrganizationJpaRepository organizationRepository) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        if (organization.isDeleted()) {
            throw new OrganizationAlreadyArchivedException(organizationId);
        }
        return organization;
    }

    public static void validateOrganizationId(Long organizationId) {
        if (organizationId == null || organizationId <= 0) {
            throw new InvalidOrganizationIdException(organizationId);
        }
    }

    // User related methods
    public static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new InvalidUserIdException(userId);
        }
    }

    public static UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new InvalidUserFieldException("role", "must not be blank");
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidUserFieldException("role", "unsupported value '" + role + "'");
        }
    }

    public static UserRole parseRoleNullable(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidUserFieldException("role", "unsupported value '" + role + "'");
        }
    }

    public static UserStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return UserStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidUserFieldException("status", "unsupported value '" + status + "'");
        }
    }

    public static void validatePageable(Pageable pageable) {
        if (pageable == null) {
            throw new InvalidUserFieldException("page", "pageable is required");
        }
        if (pageable.getPageNumber() < 0) {
            throw new InvalidUserFieldException("page", "must be greater than or equal to 0");
        }
        int size = pageable.getPageSize();
        if (size <= 0 || size > 100) {
            throw new InvalidUserFieldException("size", "must be between 1 and 100");
        }

        Set<String> allowedSort = Set.of("id", "name", "email", "role", "status", "createdAt", "updatedAt");
        for (Sort.Order order : pageable.getSort()) {
            if (!allowedSort.contains(order.getProperty())) {
                throw new InvalidUserFieldException("sort", "unsupported property '" + order.getProperty() + "'");
            }
        }
    }

    public static String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception ex) {
            throw new InvalidUserFieldException("password", "unable to hash password");
        }
    }

    public static boolean verifyPassword(String rawPassword, String expectedHash) {
        return hashPassword(rawPassword).equals(expectedHash);
    }

    public static ZoneId parseTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            throw new InvalidTimezoneException(timezone);
        }
        try {
            return ZoneId.of(timezone.trim());
        } catch (Exception e) {
            throw new InvalidTimezoneException(timezone, e);
        }
    }

    public static Currency parseCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new InvalidCurrencyException(currencyCode);
        }
        try {
            return Currency.getInstance(currencyCode.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new InvalidCurrencyException(currencyCode, e);
        }
    }

    public static OrganizationStatus parseOrganizationStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrganizationStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidOrganizationFieldException("status", "unsupported value '" + status + "'");
        }
    }

    public static void validateDateRange(LocalDate createdFrom, LocalDate createdTo) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new InvalidOrganizationFieldException("createdFrom", "must be before or equal to createdTo");
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

    // Invoice related methods

    public static void addItems(Invoice invoice, List<InvoiceItemRequest> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        for (InvoiceItemRequest item : items) {
            invoice.addItem(item.getDescription(), item.getQuantity(), item.getUnitPrice());
        }
    }

    public static void validateCreatedByUserId(Long createdByUserId) {
        if (createdByUserId == null || createdByUserId <= 0) {
            throw new InvalidUserIdException(createdByUserId);
        }
    }
}
