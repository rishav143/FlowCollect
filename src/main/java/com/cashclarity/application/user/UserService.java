package com.cashclarity.application.user;

import com.cashclarity.api.v1.user.dto.UserCreateRequest;
import com.cashclarity.api.v1.user.dto.UserPasswordChangeRequest;
import com.cashclarity.api.v1.user.dto.UserUpdateRequest;
import com.cashclarity.application.util;
import com.cashclarity.domain.organization.Organization;
import com.cashclarity.domain.user.User;
import com.cashclarity.domain.user.UserRole;
import com.cashclarity.domain.user.UserStatus;
import com.cashclarity.exception.user.InvalidUserFieldException;
import com.cashclarity.exception.user.UserAlreadyActiveException;
import com.cashclarity.exception.user.UserAlreadyExistsException;
import com.cashclarity.exception.user.UserAlreadyInactiveException;
import com.cashclarity.exception.user.UserNotFoundException;
import com.cashclarity.infrastructure.persistence.organization.OrganizationJpaRepository;
import com.cashclarity.infrastructure.persistence.user.UserJpaRepository;
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
    public User create(Long organizationId, UserCreateRequest request) {
        util.validateOrganizationId(organizationId);
        Organization organization = util.getOrganizationOrThrow(organizationId, organizationRepository);

        String email = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmailAndOrganizationId(email, organizationId)) {
            throw new UserAlreadyExistsException(email, organizationId);
        }

        UserRole role = util.parseRole(request.getRole());
        String passwordHash = util.hashPassword(request.getPassword());

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
        util.validateOrganizationId(organizationId);
        util.getOrganizationOrThrow(organizationId, organizationRepository);
        util.validatePageable(pageable);

        UserStatus parsedStatus = util.parseStatus(status);
        UserRole parsedRole = util.parseRoleNullable(role);

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
        util.validateOrganizationId(organizationId);
        util.validateUserId(userId);
        util.getOrganizationOrThrow(organizationId, organizationRepository);
        return userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new UserNotFoundException(userId, organizationId));
    }

    @Transactional
    public User update(Long organizationId, Long userId, UserUpdateRequest request) {
        util.validateOrganizationId(organizationId);
        util.validateUserId(userId);
        util.getOrganizationOrThrow(organizationId, organizationRepository);
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
            UserRole role = util.parseRole(request.getRole());
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
        util.validateOrganizationId(organizationId);
        util.validateUserId(userId);
        util.getOrganizationOrThrow(organizationId, organizationRepository);
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
        util.validateOrganizationId(organizationId);
        util.validateUserId(userId);
        util.getOrganizationOrThrow(organizationId, organizationRepository);
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
        util.validateOrganizationId(organizationId);
        util.validateUserId(userId);
        util.getOrganizationOrThrow(organizationId, organizationRepository);
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
            if (!util.verifyPassword(oldPassword, user.getPasswordHash())) {
                throw new InvalidUserFieldException("oldPassword", "does not match current password");
            }
        }

        String newPasswordHash = util.hashPassword(newPassword);
        if (newPasswordHash.equals(user.getPasswordHash())) {
            throw new InvalidUserFieldException("newPassword", "must be different from current password");
        }

        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);
    }

    @Transactional
    public void delete(Long organizationId, Long userId) {
        util.validateOrganizationId(organizationId);
        util.validateUserId(userId);
        util.getOrganizationOrThrow(organizationId, organizationRepository);
        User user = userRepository.findByIdAndOrganizationId(userId, organizationId)
                .orElseThrow(() -> new UserNotFoundException(userId, organizationId));
        userRepository.delete(user);
    }
}
