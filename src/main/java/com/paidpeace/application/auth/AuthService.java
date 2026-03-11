package com.paidpeace.application.auth;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paidpeace.api.v1.auth.dto.LoginRequest;
import com.paidpeace.api.v1.auth.dto.LoginResponse;
import com.paidpeace.api.v1.auth.dto.RegisterRequest;
import com.paidpeace.api.v1.organization.dto.OrganizationCreateRequest;
import com.paidpeace.api.v1.user.dto.UserCreateRequest;
import com.paidpeace.application.organization.OrganizationService;
import com.paidpeace.application.user.UserService;
import com.paidpeace.application.user.UserUtil;
import com.paidpeace.domain.organization.Organization;
import com.paidpeace.domain.user.User;
import com.paidpeace.domain.user.UserStatus;
import com.paidpeace.exception.http.UnauthorizedException;
import com.paidpeace.exception.http.ValidationException;
import com.paidpeace.infrastructure.persistence.user.UserJpaRepository;
import com.paidpeace.security.JwtService;

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

        Organization organization = organizationService.create(orgReq);

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
