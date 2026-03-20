package com.flowcollect.application.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.flowcollect.domain.user.UserStatus;
import com.flowcollect.exception.http.NotFoundException;
import com.flowcollect.exception.http.UnauthorizedException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.user.UserJpaRepository;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

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
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        String password = request.getPassword();

        if (email.isBlank()) {
            throw new ValidationException("Email must not be blank");
        }
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password must not be blank");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (user.getOrganization().isDeleted()) {
            throw new UnauthorizedException("Organization is archived");
        }

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
    public void resendVerificationEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email must not be blank");
        }

        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new NotFoundException("No account found for this email"));

        if (user.getStatus() != UserStatus.PENDING_EMAIL_VERIFICATION) {
            throw new ValidationException("Email is already verified");
        }

        verificationService.sendVerificationEmail(user);
    }

    @Transactional
    public void verifyPhone(String email, String otp) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email must not be blank");
        }
        if (otp == null || otp.isBlank()) {
            throw new ValidationException("OTP must not be blank");
        }

        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new NotFoundException("No account found for this email"));

        Organization organization = user.getOrganization();
        verificationService.validatePhoneOtp(organization.getId(), otp);
        organization.verifyPhone();
        organizationService.save(organization);
    }

    /**
     * Initiates a password reset for the given email.
     * Always returns silently — never reveals whether the email exists (prevents user enumeration).
     */
    @Transactional
    public void forgotPassword(String email) {
        if (email == null || email.isBlank()) {
            return;
        }
        try {
            userRepository.findByEmail(email.trim().toLowerCase())
                    .ifPresent(user -> {
                        if (user.getOrganization().isDeleted()) {
                            return;
                        }
                        // OAuth-only accounts have no password — skip silently
                        if (user.getPasswordHash() != null) {
                            verificationService.sendPasswordResetEmail(user);
                        }
                    });
        } catch (Exception ex) {
            log.warn("forgotPassword silently failed for email={}: {}", email, ex.getMessage());
        }
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.isBlank()) {
            throw new ValidationException("Token must not be blank");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new ValidationException("New password must not be blank");
        }
        if (newPassword.length() < 8 || newPassword.length() > 100) {
            throw new ValidationException("Password must be between 8 and 100 characters");
        }

        User user = verificationService.validatePasswordResetToken(token);

        if (user.getPasswordHash() != null && UserUtil.verifyPassword(newPassword, user.getPasswordHash())) {
            throw new ValidationException("New password must be different from your current password");
        }

        user.setPasswordHash(UserUtil.hashPassword(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void resendPhoneOtp(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email must not be blank");
        }

        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new NotFoundException("No account found for this email"));

        Organization organization = user.getOrganization();
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
