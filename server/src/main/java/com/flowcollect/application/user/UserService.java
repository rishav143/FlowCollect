package com.flowcollect.application.user;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowcollect.api.v1.user.dto.UserCreateRequest;
import com.flowcollect.api.v1.user.dto.UserPasswordChangeRequest;
import com.flowcollect.api.v1.user.dto.UserUpdateRequest;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.common.PaginationUtils;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.user.User;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.domain.user.UserStatus;
import com.flowcollect.exception.http.ConflictException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.user.UserJpaRepository;

@Service
public class UserService {

    private final UserJpaRepository userRepository;
    private final OrganizationService organizationService;

    public UserService
    (
        UserJpaRepository userRepository, 
        OrganizationService organizationService
    ) {
        this.userRepository = userRepository;
        this.organizationService = organizationService;
    }

    @Transactional
    public User create
    (
        UUID organizationId, 
        UserCreateRequest request
    ) {
        if(request == null) {
            throw new ValidationException( 
                "Request must not be null");
        }
        // validate and get organization from organization service
        Organization organization = organizationService.getById(organizationId);

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailAndOrganizationId(email, organizationId)) {
            throw new ConflictException( 
                "Email " + email + " must be unique within organization with ID: " + organizationId);
        }
        if(request.getName() == null || request.getName().isBlank()) {
            throw new ValidationException( 
                "Name must not be blank");
        }
        if(request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ValidationException( 
                "Password must not be blank");
        }
        if(request.getRole() == null || request.getRole().isBlank()) {
            throw new ValidationException( 
                "Role must not be blank");
        }

        UserRole role = UserUtil.parseRole(request.getRole());
        String passwordHash = UserUtil.hashPassword(request.getPassword());

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
            UUID organizationId,
            String status,
            String email,
            String name,
            String role,
            Pageable pageable
    ) {
        organizationService.getById(organizationId);
        PaginationUtils.validatePageable(pageable);

        UserStatus parsedStatus = UserUtil.parseStatus(status);
        UserRole parsedRole = UserUtil.parseRoleNullable(role);

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
    public User getById(UUID organizationId, UUID userId) {
        organizationService.getById(organizationId);
        return UserUtil.validateUserWithOrganization(userId, organizationId, userRepository);
    }

    @Transactional
    public User update
    (
        UUID organizationId, 
        UUID userId,
        UserUpdateRequest request
    ) {
        if (request == null) {
            throw new ValidationException( 
                "Request must not be null");
        }
        organizationService.getById(organizationId);
        User user = UserUtil.validateUserWithOrganization(userId, organizationId, userRepository);

        boolean changed = false;

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isBlank()) {
                throw new ValidationException( 
                    "Name must not be blank");
            }
            if (!name.equals(user.getName())) {
                user.setName(name);
                changed = true;
            }
        }

        if (request.getEmail() != null) {
            String email = request.getEmail().trim().toLowerCase();
            if (email.isBlank()) {
                throw new ValidationException( 
                    "Email must not be blank");
            }
            if (!email.equals(user.getEmail())
                    && userRepository.existsByEmailAndOrganizationIdAndIdNot(email, organizationId, userId)) {
                throw new ConflictException( 
                    "Email " + email + " must be unique within organization with ID: " + organizationId);
            }
            if (!email.equals(user.getEmail())) {
                user.setEmail(email);
                changed = true;
            }
        }

        if (request.getRole() != null) {
            UserRole role = UserUtil.parseRole(request.getRole());
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
    public User activate
    (
        UUID organizationId, 
        UUID userId
    ) {
        organizationService.getById(organizationId);
        User user = UserUtil.validateUserWithOrganization(userId, organizationId, userRepository);
        user.activate();
        return userRepository.save(user);
    }

    @Transactional
    public User deactivate(UUID organizationId, UUID userId) {
        organizationService.getById(organizationId);
        User user = UserUtil.validateUserWithOrganization(userId, organizationId, userRepository);

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new ConflictException( 
                "User with ID: " + userId + " is already inactive");
        }

        user.deactivate();
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(UUID organizationId, UUID userId, UserPasswordChangeRequest request) {
        organizationService.getById(organizationId);
        User user = UserUtil.validateUserWithOrganization(userId, organizationId, userRepository);

        if (request == null) {
            throw new ValidationException( 
                "Request must not be null");
        }

        String newPassword = request.getNewPassword();
        if (newPassword == null || newPassword.isBlank()) {
            throw new ValidationException( 
                "New password must not be blank");
        }
        if (newPassword.length() < 8 || newPassword.length() > 100) {
            throw new ValidationException( 
                "New password must be between 8 and 100 characters");
        }

        String oldPassword = request.getOldPassword();
        if (oldPassword != null) {
            if (oldPassword.isBlank()) {
                throw new ValidationException( 
                    "Old password must not be blank when provided");
            }
            if (!UserUtil.verifyPassword(oldPassword, user.getPasswordHash())) {
                throw new ValidationException( 
                    "Old password does not match current password");
            }
        }

        String newPasswordHash = UserUtil.hashPassword(newPassword);
        if (newPasswordHash.equals(user.getPasswordHash())) {
            throw new ValidationException( 
                "New password must be different from current password");
        }

        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);
    }

    @Transactional
    public void delete(UUID organizationId, UUID userId) {
        organizationService.getById(organizationId);
        User user = UserUtil.validateUserWithOrganization(userId, organizationId, userRepository);
        userRepository.delete(user);
    }
}
