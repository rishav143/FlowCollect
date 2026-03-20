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

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepository, organizationService, userService, loginResponseFactory, verificationService);
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
        UUID orgId = UUID.randomUUID();
        LoginRequest request = new LoginRequest();
        request.setOrganizationId(orgId);
        request.setEmail("owner@test.com");
        request.setPassword("password123");

        Organization org = mock(Organization.class);
        when(org.isDeleted()).thenReturn(false);
        when(organizationService.getById(orgId)).thenReturn(org);

        User user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatus.PENDING_EMAIL_VERIFICATION);
        when(userRepository.findByEmailAndOrganizationId("owner@test.com", orgId)).thenReturn(Optional.of(user));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> authService.login(request));
        assertTrue(ex.getMessage().toLowerCase().contains("verify your email"));
    }

    @Test
    void login_success_returnsToken() {
        UUID orgId = UUID.randomUUID();
        LoginRequest request = new LoginRequest();
        request.setOrganizationId(orgId);
        request.setEmail("owner@test.com");
        request.setPassword("password123");

        Organization org = mock(Organization.class);
        when(org.isDeleted()).thenReturn(false);
        when(organizationService.getById(orgId)).thenReturn(org);

        // Need a real password hash for verification
        String hash = com.flowcollect.application.user.UserUtil.hashPassword("password123");
        User user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(user.getPasswordHash()).thenReturn(hash);
        when(userRepository.findByEmailAndOrganizationId("owner@test.com", orgId)).thenReturn(Optional.of(user));

        LoginResponse expected = new LoginResponse();
        expected.setToken("jwt-token");
        when(loginResponseFactory.create(user)).thenReturn(expected);

        LoginResponse response = authService.login(request);

        assertEquals("jwt-token", response.getToken());
        verify(loginResponseFactory).create(user);
    }

    @Test
    void login_inactiveUser_throwsUnauthorized() {
        UUID orgId = UUID.randomUUID();
        LoginRequest request = new LoginRequest();
        request.setOrganizationId(orgId);
        request.setEmail("owner@test.com");
        request.setPassword("password123");

        Organization org = mock(Organization.class);
        when(org.isDeleted()).thenReturn(false);
        when(organizationService.getById(orgId)).thenReturn(org);

        User user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatus.INACTIVE);
        when(userRepository.findByEmailAndOrganizationId("owner@test.com", orgId)).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }

    @Test
    void login_nullRequest_throwsValidationException() {
        assertThrows(Exception.class, () -> authService.login(null));
    }

    @Test
    void login_archivedOrg_throwsUnauthorized() {
        UUID orgId = UUID.randomUUID();
        LoginRequest request = new LoginRequest();
        request.setOrganizationId(orgId);
        request.setEmail("owner@test.com");
        request.setPassword("password123");

        Organization org = mock(Organization.class);
        when(org.isDeleted()).thenReturn(true);
        when(organizationService.getById(orgId)).thenReturn(org);

        assertThrows(UnauthorizedException.class, () -> authService.login(request));
    }
}
