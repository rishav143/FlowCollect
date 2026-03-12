package com.flowcollect.application.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowcollect.api.v1.auth.dto.LoginRequest;
import com.flowcollect.api.v1.auth.dto.LoginResponse;
import com.flowcollect.api.v1.auth.dto.RegisterRequest;
import com.flowcollect.api.v1.organization.dto.OrganizationCreateRequest;
import com.flowcollect.api.v1.user.dto.UserCreateRequest;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.application.user.UserService;
import com.flowcollect.application.user.UserUtil;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.user.User;
import com.flowcollect.domain.user.UserStatus;
import com.flowcollect.exception.http.UnauthorizedException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.user.UserJpaRepository;
import com.flowcollect.security.JwtService;

@Service
public class AuthService {

    private final UserJpaRepository userRepository;
    private final OrganizationService organizationService;
    private final UserService userService;
    private final JwtService jwtService;

    public AuthService(
            UserJpaRepository userRepository,
            OrganizationService organizationService,
            UserService userService,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.organizationService = organizationService;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        if (request == null) {
            throw new ValidationException("Request must not be null");
        }
        UUID organizationId = request.getOrganizationId();
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String password = request.getPassword();

        if (organizationId == null) {
            throw new ValidationException("Organization ID must not be null");
        }
        if (email.isBlank()) {
            throw new ValidationException("Email must not be blank");
        }
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password must not be blank");
        }

        Organization organization = organizationService.getById(organizationId);
        if (organization.isDeleted()) {
            throw new UnauthorizedException("Organization is archived");
        }

        User user = userRepository.findByEmailAndOrganizationId(email, organizationId)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("User account is inactive");
        }
        if (!UserUtil.verifyPassword(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return buildLoginResponse(user);
    }

    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (request == null) {
            throw new ValidationException("Request must not be null");
        }

        // 1. Create Organization
        OrganizationCreateRequest orgReq = new OrganizationCreateRequest();
        orgReq.setName(request.getOrganizationName());
        orgReq.setEmail(request.getEmail());
        orgReq.setCurrency(request.getCurrency());
        orgReq.setTimezone(request.getTimezone());

        Organization organization = organizationService.createForRegistration(orgReq);

        // 2. Create Owner User
        UserCreateRequest userReq = new UserCreateRequest();
        userReq.setName(request.getOwnerName());
        userReq.setEmail(request.getEmail());
        userReq.setPassword(request.getPassword());
        userReq.setRole("ADMIN");

        User user = userService.create(organization.getId(), userReq);

        // 3. Return Token
        return buildLoginResponse(user);
    }

    private LoginResponse buildLoginResponse(User user) {
        String token = jwtService.createToken(user.getId(), user.getOrganization().getId(), user.getRole());
        Instant expiresAt = Instant.now().plusMillis(jwtService.getExpirationMs());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setType("Bearer");
        response.setId(user.getId());
        response.setOrganizationId(user.getOrganization().getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setExpiresAt(expiresAt);
        return response;
    }
}
