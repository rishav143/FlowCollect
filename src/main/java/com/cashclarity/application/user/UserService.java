package com.cashclarity.application.user;

import com.cashclarity.api.v1.user.dto.UserCreateRequest;
import com.cashclarity.api.v1.user.dto.UserPasswordChangeRequest;
import com.cashclarity.api.v1.user.dto.UserUpdateRequest;
import com.cashclarity.application.organization.OrganizationUtil;
import com.cashclarity.common.PaginationUtils;
import com.cashclarity.domain.organization.Organization;
import com.cashclarity.domain.user.User;
import com.cashclarity.domain.user.UserRole;
import com.cashclarity.domain.user.UserStatus;
import com.cashclarity.exception.user.InvalidUserFieldException;
import com.cashclarity.exception.user.UserAlreadyExistsException;
import com.cashclarity.infrastructure.persistence.organization.OrganizationJpaRepository;
import com.cashclarity.infrastructure.persistence.user.UserJpaRepository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserJpaRepository userRepository;
    private final OrganizationJpaRepository organizationRepository;

    public UserService(UserJpaRepository userRepository, OrganizationJpaRepository organizationRepository) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public User create(UUID organizationId, UserCreateRequest request) {
        Organization organization = OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailAndOrganizationId(email, organizationId)) {
            throw new UserAlreadyExistsException("email " + email + " must be unique within organization " + organizationId);
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
        OrganizationUtil.getOrganizationOrThrow(organizationId, organizationRepository);
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
        return UserUtil.validateUserWithOrganization(userId, organizationId, userRepository);
    }

    @Transactional
    public User update(UUID organizationId, UUID userId, UserUpdateRequest request) {
        if (request == null) {
            throw new InvalidUserFieldException("request must not be null");
        }
        User user = UserUtil.validateUserWithOrganization(userId, organizationId, userRepository);

        boolean changed = false;

        if (request.getName() != null) {
            String name = request.getName().trim();
            if (name.isBlank()) {
                throw new InvalidUserFieldException("name must not be blank");
            }
            if (!name.equals(user.getName())) {
                user.setName(name);
                changed = true;
            }
        }

        if (request.getEmail() != null) {
            String email = request.getEmail().trim().toLowerCase();
            if (email.isBlank()) {
                throw new InvalidUserFieldException("email must not be blank");
            }
            if (!email.equals(user.getEmail())
                    && userRepository.existsByEmailAndOrganizationIdAndIdNot(email, organizationId, userId)) {
                throw new UserAlreadyExistsException("email must be unique within organization");
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
    public User activate(UUID organizationId, UUID userId) {
        User user = UserUtil.validateUserWithOrganization(userId, organizationId, userRepository);
        user.activate();
        return userRepository.save(user);
    }

    @Transactional
    public User deactivate(UUID organizationId, UUID userId) {
        User user = UserUtil.validateUserWithOrganization(userId, organizationId, userRepository);

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new UserAlreadyExistsException("user is already inactive");
        }

        user.deactivate();
        return userRepository.save(user);
    }

    @Transactional
    public void changePassword(UUID organizationId, UUID userId, UserPasswordChangeRequest request) {
        User user = UserUtil.validateUserWithOrganization(userId, organizationId, userRepository);

        if (request == null) {
            throw new InvalidUserFieldException("request must not be null");
        }

        String newPassword = request.getNewPassword();
        if (newPassword == null || newPassword.isBlank()) {
            throw new InvalidUserFieldException("newPassword must not be blank");
        }
        if (newPassword.length() < 8 || newPassword.length() > 100) {
            throw new InvalidUserFieldException("newPassword must be between 8 and 100 characters");
        }

        String oldPassword = request.getOldPassword();
        if (oldPassword != null) {
            if (oldPassword.isBlank()) {
                throw new InvalidUserFieldException("oldPassword must not be blank when provided");
            }
            if (!UserUtil.verifyPassword(oldPassword, user.getPasswordHash())) {
                throw new InvalidUserFieldException("oldPassword does not match current password");
            }
        }

        String newPasswordHash = UserUtil.hashPassword(newPassword);
        if (newPasswordHash.equals(user.getPasswordHash())) {
            throw new InvalidUserFieldException("newPassword must be different from current password");
        }

        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);
    }

    @Transactional
    public void delete(UUID organizationId, UUID userId) {
        User user = UserUtil.validateUserWithOrganization(userId, organizationId, userRepository);
        userRepository.delete(user);
    }
}
