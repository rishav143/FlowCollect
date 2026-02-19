package com.cashclarity.application.user;

import com.cashclarity.domain.user.User;
import com.cashclarity.domain.user.UserRole;
import com.cashclarity.domain.user.UserStatus;
import com.cashclarity.exception.user.InvalidUserFieldException;
import com.cashclarity.exception.user.UserNotFoundException;
import com.cashclarity.infrastructure.persistence.user.UserJpaRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

import com.cashclarity.exception.organization.InvalidOrganizationFieldException;

public class UserUtil {
    
    public static User getUserOrThrow(UUID userId, UserJpaRepository userRepository) {
        if (userId == null) {
            throw new InvalidUserFieldException("userId with value " + userId + " must not be null");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("user not found with id " + userId));
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new UserNotFoundException("user is inactive with id " + userId);
        }
        return user;
    }

    public static User validateUserWithOrganization(UUID userId, UUID organizationId, UserJpaRepository userRepository) {
        if (userId == null) {
            throw new InvalidUserFieldException("userId with value " + userId + " must not be null");
        }
        if (organizationId == null) {
            throw new InvalidOrganizationFieldException("organizationId with value " + organizationId + " must not be null");
        }
        User user = getUserOrThrow(userId, userRepository);
        if (user.getOrganization().isDeleted()) {
            throw new InvalidOrganizationFieldException("organization is archived with id " + organizationId);
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new UserNotFoundException("user is inactive with id " + userId);
        }
        if (user.getOrganization().getId() != organizationId) {
            throw new InvalidUserFieldException("user is not associated with organization with id " + organizationId);
        }
        return user;
    }

    public static UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new InvalidUserFieldException("role with value " + role + " must not be null or blank");
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidUserFieldException("unsupported role with value '" + role + "'");
        }
    }

    public static UserRole parseRoleNullable(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidUserFieldException("unsupported role value '" + role + "'");
        }
    }

    public static UserStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return UserStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidUserFieldException("unsupported status value '" + status + "'");
        }
    }

    public static String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception ex) {
            throw new InvalidOrganizationFieldException("unable to hash password");
        }
    }

    public static boolean verifyPassword(String rawPassword, String expectedHash) {
        return hashPassword(rawPassword).equals(expectedHash);
    }
}
