package com.cashclarity.application.user;

import com.cashclarity.api.v1.user.dto.UserCreateRequest;
import com.cashclarity.api.v1.user.dto.UserPasswordChangeRequest;
import com.cashclarity.api.v1.user.dto.UserUpdateRequest;
import com.cashclarity.domain.organization.Organization;
import com.cashclarity.domain.user.User;
import com.cashclarity.domain.user.UserRole;
import com.cashclarity.domain.user.UserStatus;
import com.cashclarity.exception.organization.InvalidOrganizationIdException;
import com.cashclarity.exception.user.InvalidUserFieldException;
import com.cashclarity.exception.user.InvalidUserIdException;
import com.cashclarity.exception.organization.OrganizationAlreadyArchivedException;
import com.cashclarity.exception.organization.OrganizationNotFoundException;
import com.cashclarity.exception.user.UserAlreadyActiveException;
import com.cashclarity.exception.user.UserAlreadyExistsException;
import com.cashclarity.exception.user.UserAlreadyInactiveException;
import com.cashclarity.exception.user.UserNotFoundException;
import com.cashclarity.infrastructure.persistence.organization.OrganizationJpaRepository;
import com.cashclarity.infrastructure.persistence.user.UserJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Set;
import java.util.Base64;

@Service
public class UserService {

    private final UserJpaRepository userRepository;
    private final OrganizationJpaRepository organizationRepository;

    public UserService(UserJpaRepository userRepository, OrganizationJpaRepository organizationRepository) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public User create(Long organizationId, UserCreateRequest request) {
        validateOrganizationId(organizationId);
        Organization organization = getOrganizationOrThrow(organizationId);

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailAndOrganizationId(email, organizationId)) {
            throw new UserAlreadyExistsException(email, organizationId);
        }

        UserRole role = parseRole(request.getRole());
        String passwordHash = hashPassword(request.getPassword());

        User user = new User(
                organization,
                request.getName().trim(),
                email,
                passwordHash,
                role
        );

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Page<User> list(
            Long organizationId,
            String status,
            String email,
            String name,
            String role,
            Pageable pageable
    ) {
        validateOrganizationId(organizationId);
        getOrganizationOrThrow(organizationId);
        validatePageable(pageable);

        UserStatus parsedStatus = parseStatus(status);
        UserRole parsedRole = parseRoleNullable(role);

        Specification<User> spec = (root, query, cb) -> cb.conjunction();
        spec = spec.and((root, query, cb) -> cb.equal(root.get("organization").get("id"), organizationId));

        if (parsedStatus != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), parsedStatus));
        }
        if (parsedRole != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), parsedRole));
        }
        if (email != null && !email.isBlank()) {
            String like = "%" + email.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("email")), like));
        }
        if (name != null && !name.isBlank()) {
            String like = "%" + name.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("name")), like));
        }

        return userRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public User getById(Long organizationId, Long userId) {
        validateOrganizationId(organizationId);
        validateUserId(userId);
        getOrganizationOrThrow(organizationId);
        return userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new UserNotFoundException(userId, organizationId));
    }

    @Transactional
    public User update(Long organizationId, Long userId, UserUpdateRequest request) {
        validateOrganizationId(organizationId);
        validateUserId(userId);
        getOrganizationOrThrow(organizationId);
        User user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new UserNotFoundException(userId, organizationId));

        if (request == null) {
            throw new InvalidUserFieldException("request", "must not be null");
        }

        boolean changed = false;

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isBlank()) {
                throw new InvalidUserFieldException("name", "must not be blank");
            }
            if (!name.equals(user.getName())) {
                user.setName(name);
                changed = true;
            }
        }

        if (request.getEmail() != null) {
            String email = request.getEmail().trim().toLowerCase();
            if (email.isBlank()) {
                throw new InvalidUserFieldException("email", "must not be blank");
            }
            if (!email.equals(user.getEmail())
                    && userRepository.existsByEmailAndOrganizationIdAndIdNot(email, organizationId, userId)) {
                throw new UserAlreadyExistsException(email, organizationId);
            }
            if (!email.equals(user.getEmail())) {
                user.setEmail(email);
                changed = true;
            }
        }

        if (request.getRole() != null) {
            UserRole role = parseRole(request.getRole());
            if (role != user.getRole()) {
                user.setRole(role);
                changed = true;
            }
        }

        if (!changed) {
            return user;
        }

        return userRepository.save(user);
    }

    @Transactional
    public User activate(Long organizationId, Long userId) {
        validateOrganizationId(organizationId);
        validateUserId(userId);
        getOrganizationOrThrow(organizationId);
        User user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new UserNotFoundException(userId, organizationId));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new UserAlreadyActiveException(userId, organizationId);
        }

        user.activate();
        return userRepository.save(user);
    }

    @Transactional
    public User deactivate(Long organizationId, Long userId) {
        validateOrganizationId(organizationId);
        validateUserId(userId);
        getOrganizationOrThrow(organizationId);
        User user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new UserNotFoundException(userId, organizationId));

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new UserAlreadyInactiveException(userId, organizationId);
        }

        user.deactivate();
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(Long organizationId, Long userId, UserPasswordChangeRequest request) {
        validateOrganizationId(organizationId);
        validateUserId(userId);
        getOrganizationOrThrow(organizationId);
        User user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new UserNotFoundException(userId, organizationId));

        if (request == null) {
            throw new InvalidUserFieldException("request", "must not be null");
        }

        String newPassword = request.getNewPassword();
        if (newPassword == null || newPassword.isBlank()) {
            throw new InvalidUserFieldException("newPassword", "must not be blank");
        }
        if (newPassword.length() < 8 || newPassword.length() > 100) {
            throw new InvalidUserFieldException("newPassword", "must be between 8 and 100 characters");
        }

        String oldPassword = request.getOldPassword();
        if (oldPassword != null) {
            if (oldPassword.isBlank()) {
                throw new InvalidUserFieldException("oldPassword", "must not be blank when provided");
            }
            if (!verifyPassword(oldPassword, user.getPasswordHash())) {
                throw new InvalidUserFieldException("oldPassword", "does not match current password");
            }
        }

        String newPasswordHash = hashPassword(newPassword);
        if (newPasswordHash.equals(user.getPasswordHash())) {
            throw new InvalidUserFieldException("newPassword", "must be different from current password");
        }

        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);
    }

    @Transactional
    public void delete(Long organizationId, Long userId) {
        validateOrganizationId(organizationId);
        validateUserId(userId);
        getOrganizationOrThrow(organizationId);
        User user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new UserNotFoundException(userId, organizationId));
        userRepository.delete(user);
    }

    private Organization getOrganizationOrThrow(Long organizationId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        if (organization.isDeleted()) {
            throw new OrganizationAlreadyArchivedException(organizationId);
        }
        return organization;
    }

    private void validateOrganizationId(Long organizationId) {
        if (organizationId == null || organizationId <= 0) {
            throw new InvalidOrganizationIdException(organizationId);
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new InvalidUserIdException(userId);
        }
    }

    private UserRole parseRole(String role) {
        if (role == null || role.isBlank()) {
            throw new InvalidUserFieldException("role", "must not be blank");
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidUserFieldException("role", "unsupported value '" + role + "'");
        }
    }

    private UserRole parseRoleNullable(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidUserFieldException("role", "unsupported value '" + role + "'");
        }
    }

    private UserStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return UserStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new InvalidUserFieldException("status", "unsupported value '" + status + "'");
        }
    }

    private void validatePageable(Pageable pageable) {
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

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (Exception ex) {
            throw new InvalidUserFieldException("password", "unable to hash password");
        }
    }

    private boolean verifyPassword(String rawPassword, String expectedHash) {
        return hashPassword(rawPassword).equals(expectedHash);
    }
}
