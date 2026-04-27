package com.flowcollect.application.auth;

import com.flowcollect.api.v1.auth.dto.LoginRequest;
import com.flowcollect.api.v1.auth.dto.LoginResponse;
import com.flowcollect.api.v1.auth.dto.RegisterRequest;
import com.flowcollect.api.v1.auth.dto.RegisterResponse;
import com.flowcollect.api.v1.organization.dto.OrganizationCreateRequest;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.application.user.UserService;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.user.User;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.domain.user.UserStatus;
import com.flowcollect.exception.http.UnauthorizedException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.config.OrgDefaultDataSeeder;
import com.flowcollect.infrastructure.persistence.user.UserJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock private UserJpaRepository userRepository;
    @Mock private OrganizationService organizationService;
    @Mock private UserService userService;
    @Mock private LoginResponseFactory loginResponseFactory;
    @Mock private VerificationService verificationService;
    @Mock private OrgDefaultDataSeeder orgDefaultDataSeeder;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepository, organizationService, userService, loginResponseFactory, verificationService, orgDefaultDataSeeder);
    }

    // -----------------------------------------------------------------------
    // register()
    // -----------------------------------------------------------------------

    @Test
    void register_success_returnsRegisterResponse_andSendsVerificationEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setOrganizationName("Test Org");
        request.setEmail("owner@test.com");
        request.setCurrency("USD");
        request.setTimezone("UTC");
        request.setOwnerName("Owner Name");
        request.setPassword("password123");

        UUID orgId = UUID.randomUUID();
        Organization org = mock(Organization.class);
        when(org.getId()).thenReturn(orgId);
        when(organizationService.createForRegistration(any(OrganizationCreateRequest.class))).thenReturn(org);

        User user = mock(User.class);
        UUID userId = UUID.randomUUID();
        when(user.getId()).thenReturn(userId);
        when(userService.createPending(eq(org), eq("Owner Name"), eq("owner@test.com"), eq("password123"))).thenReturn(user);

        RegisterResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(orgId, response.getOrganizationId());
        assertEquals(userId, response.getUserId());
        assertTrue(response.isEmailVerificationRequired());
        assertFalse(response.isPhoneVerificationRequired());

        verify(organizationService).createForRegistration(any(OrganizationCreateRequest.class));
        verify(userService).createPending(org, "Owner Name", "owner@test.com", "password123");
        verify(verificationService).sendVerificationEmail(user);
        verify(verificationService, never()).sendPhoneOtp(any(), any());
        // register() must NOT issue a JWT
        verify(loginResponseFactory, never()).create(any());
    }

    @Test
    void register_withPhone_sendsPhoneOtpAndSetsFlag() {
        RegisterRequest request = new RegisterRequest();
        request.setOrganizationName("Test Org");
        request.setEmail("owner@test.com");
        request.setCurrency("USD");
        request.setTimezone("UTC");
        request.setOwnerName("Owner Name");
        request.setPassword("password123");
        request.setPhone("+1234567890");

        UUID orgId = UUID.randomUUID();
        Organization org = mock(Organization.class);
        when(org.getId()).thenReturn(orgId);
        when(organizationService.createForRegistration(any())).thenReturn(org);

        User user = mock(User.class);
        when(user.getId()).thenReturn(UUID.randomUUID());
        when(userService.createPending(any(), any(), any(), any())).thenReturn(user);

        RegisterResponse response = authService.register(request);

        assertTrue(response.isEmailVerificationRequired());
        assertTrue(response.isPhoneVerificationRequired());
        verify(verificationService).sendPhoneOtp(org, "+1234567890");
    }

    @Test
    void register_nullRequest_throwsValidationException() {
        assertThrows(Exception.class, () -> authService.register(null));
    }

    // -----------------------------------------------------------------------
    // login()
    // -----------------------------------------------------------------------

    @Test
    void login_pendingEmailVerification_throwsUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setEmail("owner@test.com");
        request.setPassword("password123");

        Organization org = mock(Organization.class);
        when(org.isDeleted()).thenReturn(false);

        User user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatus.PENDING_EMAIL_VERIFICATION);
        when(user.getOrganization()).thenReturn(org);
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login(request));
        assertTrue(ex.getMessage().toLowerCase().contains("verify your email"));
    }

    @Test
    void login_success_returnsToken() {
        LoginRequest request = new LoginRequest();
        request.setEmail("owner@test.com");
        request.setPassword("password123");

        Organization org = mock(Organization.class);
        when(org.isDeleted()).thenReturn(false);

        String hash = com.flowcollect.application.user.UserUtil.hashPassword("password123");
        User user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getPasswordHash()).thenReturn(hash);
        when(user.getOrganization()).thenReturn(org);
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user));

        LoginResponse expected = new LoginResponse();
        expected.setToken("jwt-token");
        when(loginResponseFactory.create(user)).thenReturn(expected);

        LoginResponse response = authService.login(request);

        assertEquals("jwt-token", response.getToken());
        verify(loginResponseFactory).create(user);
    }

    @Test
    void login_inactiveUser_throwsUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setEmail("owner@test.com");
        request.setPassword("password123");

        Organization org = mock(Organization.class);
        when(org.isDeleted()).thenReturn(false);

        User user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatus.INACTIVE);
        when(user.getOrganization()).thenReturn(org);
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void login_nullRequest_throwsValidationException() {
        assertThrows(Exception.class, () -> authService.login(null));
    }

    @Test
    void login_archivedOrg_throwsUnauthorized() {
        LoginRequest request = new LoginRequest();
        request.setEmail("owner@test.com");
        request.setPassword("password123");

        Organization org = mock(Organization.class);
        when(org.isDeleted()).thenReturn(true);

        User user = mock(User.class);
        when(user.getOrganization()).thenReturn(org);
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    // -----------------------------------------------------------------------
    // forgotPassword()
    // -----------------------------------------------------------------------

    @Test
    void forgotPassword_existingUser_sendsResetEmail() {
        Organization org = mock(Organization.class);
        when(org.isDeleted()).thenReturn(false);

        User user = mock(User.class);
        when(user.getPasswordHash()).thenReturn("some-hash");
        when(user.getOrganization()).thenReturn(org);
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user));

        authService.forgotPassword("owner@test.com");

        verify(verificationService).sendPasswordResetEmail(user);
    }

    @Test
    void forgotPassword_unknownEmail_doesNotThrowAndSendsNothing() {
        when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

        // Must never throw — prevents user enumeration
        assertDoesNotThrow(() -> authService.forgotPassword("ghost@test.com"));
        verify(verificationService, never()).sendPasswordResetEmail(any());
    }

    @Test
    void forgotPassword_oauthOnlyUser_doesNotSendResetEmail() {
        Organization org = mock(Organization.class);
        when(org.isDeleted()).thenReturn(false);

        User oauthUser = mock(User.class);
        when(oauthUser.getPasswordHash()).thenReturn(null); // OAuth-only — no password
        when(oauthUser.getOrganization()).thenReturn(org);
        when(userRepository.findByEmail("oauth@test.com")).thenReturn(Optional.of(oauthUser));

        assertDoesNotThrow(() -> authService.forgotPassword("oauth@test.com"));
        verify(verificationService, never()).sendPasswordResetEmail(any());
    }

    @Test
    void forgotPassword_archivedOrg_doesNotThrow() {
        Organization org = mock(Organization.class);
        when(org.isDeleted()).thenReturn(true);

        User user = mock(User.class);
        when(user.getOrganization()).thenReturn(org);
        when(userRepository.findByEmail("owner@test.com")).thenReturn(Optional.of(user));

        assertDoesNotThrow(() -> authService.forgotPassword("owner@test.com"));
        verify(verificationService, never()).sendPasswordResetEmail(any());
    }

    @Test
    void forgotPassword_nullOrBlankEmail_doesNotThrow() {
        assertDoesNotThrow(() -> authService.forgotPassword(null));
        assertDoesNotThrow(() -> authService.forgotPassword("  "));
        verify(verificationService, never()).sendPasswordResetEmail(any());
    }

    // -----------------------------------------------------------------------
    // resetPassword()
    // -----------------------------------------------------------------------

    @Test
    void resetPassword_validToken_updatesPasswordHash() {
        String newPassword = "newSecurePass1";
        User user = mock(User.class);
        when(user.getPasswordHash()).thenReturn("old-hash");
        when(verificationService.validatePasswordResetToken("valid-token")).thenReturn(user);

        authService.resetPassword("valid-token", newPassword);

        verify(user).setPasswordHash(any());
        verify(userRepository).save(user);
    }

    @Test
    void resetPassword_samePassword_throwsValidationException() {
        String password = "samePassword1";
        String existingHash = com.flowcollect.application.user.UserUtil.hashPassword(password);

        User user = mock(User.class);
        when(user.getPasswordHash()).thenReturn(existingHash);
        when(verificationService.validatePasswordResetToken("token")).thenReturn(user);

        assertThrows(ValidationException.class,
                () -> authService.resetPassword("token", password));
        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_blankToken_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> authService.resetPassword("  ", "newPassword1"));
    }

    @Test
    void resetPassword_tooShortPassword_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> authService.resetPassword("token", "short"));
    }

    @Test
    void resetPassword_nullPassword_throwsValidationException() {
        assertThrows(ValidationException.class,
                () -> authService.resetPassword("token", null));
    }
}
