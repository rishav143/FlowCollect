package com.flowcollect.application.auth;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowcollect.api.v1.auth.dto.LoginRequest;
import com.flowcollect.api.v1.auth.dto.LoginResponse;
import com.flowcollect.api.v1.auth.dto.RegisterRequest;
import com.flowcollect.api.v1.auth.dto.RegisterResponse;
import com.flowcollect.api.v1.organization.dto.OrganizationCreateRequest;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.application.user.UserService;
import com.flowcollect.application.user.UserUtil;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.user.User;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.domain.user.UserStatus;
import com.flowcollect.exception.http.NotFoundException;
import com.flowcollect.exception.http.UnauthorizedException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.user.UserJpaRepository;

@Service
public class AuthService {

    private final UserJpaRepository userRepository;
    private final OrganizationService organizationService;
    private final UserService userService;
    private final LoginResponseFactory loginResponseFactory;
    private final VerificationService verificationService;

    public AuthService(
            UserJpaRepository userRepository,
            OrganizationService organizationService,
            UserService userService,
            LoginResponseFactory loginResponseFactory,
            VerificationService verificationService
    ) {
        this.userRepository = userRepository;
        this.organizationService = organizationService;
        this.userService = userService;
        this.loginResponseFactory = loginResponseFactory;
        this.verificationService = verificationService;
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

        if (user.getStatus() == UserStatus.PENDING_EMAIL_VERIFICATION) {
            throw new UnauthorizedException("Please verify your email address before logging in");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new UnauthorizedException("User account is inactive");
        }

        if (user.getPasswordHash() == null) {
            throw new UnauthorizedException(
                    "This account was created via social login. Please sign in with Google or Microsoft.");
        }
        if (!UserUtil.verifyPassword(password, user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        return loginResponseFactory.create(user);
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (request == null) {
            throw new ValidationException("Request must not be null");
        }

        // 1. Create Organization
        OrganizationCreateRequest orgReq = new OrganizationCreateRequest();
        orgReq.setName(request.getOrganizationName());
        orgReq.setEmail(request.getEmail());
        orgReq.setCurrency(request.getCurrency());
        orgReq.setTimezone(request.getTimezone());
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            orgReq.setPhone(request.getPhone().trim());
        }

        Organization organization = organizationService.createForRegistration(orgReq);

        // 2. Create Owner User (pending email verification)
        User user = userService.createPending(
                organization,
                request.getOwnerName(),
                request.getEmail(),
                request.getPassword()
        );

        // 3. Send email verification (always required)
        verificationService.sendVerificationEmail(user);

        // 4. Send phone OTP if phone was provided
        boolean phoneVerificationRequired = false;
        String phone = request.getPhone() != null ? request.getPhone().trim() : null;
        if (phone != null && !phone.isBlank()) {
            verificationService.sendPhoneOtp(organization, phone);
            phoneVerificationRequired = true;
        }

        return new RegisterResponse(organization.getId(), user.getId(), true, phoneVerificationRequired);
    }

    @Transactional
    public LoginResponse verifyEmail(String token) {
        if (token == null || token.isBlank()) {
            throw new ValidationException("Token must not be blank");
        }

        User user = verificationService.validateEmailToken(token);
        user.activate();
        userRepository.save(user);

        return loginResponseFactory.create(user);
    }

    @Transactional
    public void resendVerificationEmail(UUID organizationId) {
        if (organizationId == null) {
            throw new ValidationException("Organization ID must not be null");
        }

        User admin = userRepository.findFirstByOrganizationIdAndRole(organizationId, UserRole.ADMIN)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        if (admin.getStatus() != UserStatus.PENDING_EMAIL_VERIFICATION) {
            throw new ValidationException("Email is already verified");
        }

        verificationService.sendVerificationEmail(admin);
    }

    @Transactional
    public void verifyPhone(UUID organizationId, String otp) {
        if (organizationId == null) {
            throw new ValidationException("Organization ID must not be null");
        }
        if (otp == null || otp.isBlank()) {
            throw new ValidationException("OTP must not be blank");
        }

        verificationService.validatePhoneOtp(organizationId, otp);

        Organization organization = organizationService.getById(organizationId);
        organization.verifyPhone();
        organizationService.save(organization);
    }

    @Transactional
    public void resendPhoneOtp(UUID organizationId) {
        if (organizationId == null) {
            throw new ValidationException("Organization ID must not be null");
        }

        Organization organization = organizationService.getById(organizationId);
        String phone = organization.getPhone();

        if (phone == null || phone.isBlank()) {
            throw new ValidationException("No phone number associated with this organization");
        }
        if (organization.isPhoneVerified()) {
            throw new ValidationException("Phone number is already verified");
        }

        verificationService.sendPhoneOtp(organization, phone);
    }
}
