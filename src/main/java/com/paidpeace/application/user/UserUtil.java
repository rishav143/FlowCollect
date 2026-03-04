package com.paidpeace.application.user;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

import com.paidpeace.domain.user.User;
import com.paidpeace.domain.user.UserRole;
import com.paidpeace.domain.user.UserStatus;
import com.paidpeace.exception.code.OrganizationErrorCode;
import com.paidpeace.exception.code.ServerErrorCode;
import com.paidpeace.exception.code.UserErrorCode;
import com.paidpeace.exception.http.ConflictException;
import com.paidpeace.exception.http.NotFoundException;
import com.paidpeace.exception.http.ServiceUnavailableException;
import com.paidpeace.exception.http.ValidationException;
import com.paidpeace.infrastructure.persistence.user.UserJpaRepository;

public class UserUtil {
    
    public static User getUserOrThrow(UUID userId, UserJpaRepository userRepository) {
        if (userId == null) {
            throw new ValidationException(UserErrorCode.INVALID_USER_FIELD,
                "User ID must not be null");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException(UserErrorCode.USER_NOT_FOUND,
                "User not found with ID: " + userId));
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new NotFoundException(UserErrorCode.USER_NOT_FOUND,
                "User with ID: " + userId + " is inactive");
        }
        return user;
    }

    public static User validateUserWithOrganization(UUID userId, UUID organizationId, UserJpaRepository userRepository) {
        if (userId == null) {
            throw new ValidationException(UserErrorCode.INVALID_USER_FIELD,
                "User ID must not be null");
        }
        if (organizationId == null) {
            throw new ValidationException(OrganizationErrorCode.INVALID_ORGANIZATION_FIELD,
                "Organization ID must not be null");
        }
        User user = getUserOrThrow(userId, userRepository);
        if (user.getOrganization().isDeleted()) {
            throw new ConflictException(OrganizationErrorCode.ORGANIZATION_ALREADY_EXISTS,
                "Organization with ID: " + organizationId + " is already archived");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new NotFoundException(UserErrorCode.USER_NOT_FOUND,
                "User with ID: " + userId + " is inactive");
        }
        if (user.getOrganization().getId() != organizationId) {
            throw new ConflictException(OrganizationErrorCode.ORGANIZATION_NOT_FOUND,
                "User with ID: " + userId + " is not associated with organization with ID: " + organizationId);
        }
        return user;
    }

    public static UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new ValidationException(UserErrorCode.INVALID_USER_FIELD,
                "Role must not be null or blank");
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(UserErrorCode.INVALID_USER_FIELD,
                "Unsupported role value '" + role + "'");
        }
    }

    public static UserRole parseRoleNullable(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(UserErrorCode.INVALID_USER_FIELD,
                "Unsupported role value '" + role + "'");
        }
    }

    public static UserStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return UserStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException(UserErrorCode.INVALID_USER_FIELD,
                "Unsupported status value '" + status + "'");
        }
    }

    public static String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception ex) {
            throw new ServiceUnavailableException(ServerErrorCode.SERVICE_UNAVAILABLE,
                "Unable to hash password due to: " + ex.getMessage());
        }
    }

    public static boolean verifyPassword(String rawPassword, String expectedHash) {
        return hashPassword(rawPassword).equals(expectedHash);
    }
}
