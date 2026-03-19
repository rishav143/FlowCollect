package com.flowcollect.application.user;

import java.util.UUID;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.flowcollect.domain.user.User;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.domain.user.UserStatus;
import com.flowcollect.exception.http.ConflictException;
import com.flowcollect.exception.http.NotFoundException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.user.UserJpaRepository;

public class UserUtil {

    public static boolean ValidateUser(User user) {
        if (user == null) {
            throw new ValidationException( 
                "User must not be null");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new NotFoundException( 
                "User with ID: " + user.getId() + " is inactive");
        }
        return true;
    }
    
    public static User getUserOrThrow(UUID userId, UserJpaRepository userRepository) {
        if (userId == null) {
            throw new ValidationException( 
                "User ID must not be null");
        }
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException( 
                "User not found with ID: " + userId));
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new NotFoundException( 
                "User with ID: " + userId + " is inactive");
        }
        return user;
    }

    public static User validateUserWithOrganization(UUID userId, UUID organizationId, UserJpaRepository userRepository) {
        if (userId == null) {
            throw new ValidationException( 
                "User ID must not be null");
        }
        if (organizationId == null) {
            throw new ValidationException( 
                "Organization ID must not be null");
        }
        User user = getUserOrThrow(userId, userRepository);
        if (user.getOrganization().isDeleted()) {
            throw new ConflictException( 
                "Organization with ID: " + organizationId + " is already archived");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new NotFoundException( 
                "User with ID: " + userId + " is inactive");
        }
        if (!user.getOrganization().getId().equals(organizationId)) {
            throw new ConflictException( 
                "User with ID: " + userId + " is not associated with organization with ID: " + organizationId);
        }
        return user;
    }

    public static UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new ValidationException( 
                "Role must not be null or blank");
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException( 
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
            throw new ValidationException( 
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
            throw new ValidationException( 
                "Unsupported status value '" + status + "'");
        }
    }

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    public static String hashPassword(String rawPassword) {
        return PASSWORD_ENCODER.encode(rawPassword);
    }

    public static boolean verifyPassword(String rawPassword, String expectedHash) {
        return PASSWORD_ENCODER.matches(rawPassword, expectedHash);
    }
}
