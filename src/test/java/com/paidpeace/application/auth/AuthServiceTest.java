package com.paidpeace.application.auth;

import com.paidpeace.api.v1.auth.dto.LoginResponse;
import com.paidpeace.api.v1.auth.dto.RegisterRequest;
import com.paidpeace.api.v1.organization.dto.OrganizationCreateRequest;
import com.paidpeace.api.v1.user.dto.UserCreateRequest;
import com.paidpeace.application.organization.OrganizationService;
import com.paidpeace.application.user.UserService;
import com.paidpeace.domain.organization.Organization;
import com.paidpeace.domain.user.User;
import com.paidpeace.domain.user.UserRole;
import com.paidpeace.domain.user.UserStatus;
import com.paidpeace.infrastructure.persistence.user.UserJpaRepository;
import com.paidpeace.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Currency;
import java.util.UUID;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    @Mock
    private UserJpaRepository userRepository;
    @Mock
    private OrganizationService organizationService;
    @Mock
    private UserService userService;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepository, organizationService, userService, jwtService);
    }

    @Test
    void register_Success() {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setOrganizationName("Test Org");
        request.setEmail("owner@test.com");
        request.setCurrency("USD");
        request.setTimezone("UTC");
        request.setOwnerName("Owner Name");
        request.setPassword("password123");

        Organization organization = new Organization("Test Org", "owner@test.com", ZoneId.of("UTC"), Currency.getInstance("USD"));
        UUID orgId = UUID.randomUUID();
        // Use reflection or a trick if id is not settable, but for mock it's easier to mock the service return
        Organization mockedOrg = mock(Organization.class);
        when(mockedOrg.getId()).thenReturn(orgId);
        when(organizationService.create(any(OrganizationCreateRequest.class))).thenReturn(mockedOrg);

        User user = mock(User.class);
        UUID userId = UUID.randomUUID();
        when(user.getId()).thenReturn(userId);
        when(user.getOrganization()).thenReturn(mockedOrg);
        when(user.getEmail()).thenReturn("owner@test.com");
        when(user.getName()).thenReturn("Owner Name");
        when(user.getRole()).thenReturn(UserRole.ADMIN);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);

        when(userService.create(eq(orgId), any(UserCreateRequest.class))).thenReturn(user);
        when(jwtService.createToken(any(), any(), any())).thenReturn("mocked-token");
        when(jwtService.getExpirationMs()).thenReturn(3600000L);

        // When
        LoginResponse response = authService.register(request);

        // Then
        assertNotNull(response);
        assertEquals("mocked-token", response.getToken());
        assertEquals("owner@test.com", response.getEmail());
        assertEquals(orgId, response.getOrganizationId());
        assertEquals(userId, response.getId());

        verify(organizationService).create(any(OrganizationCreateRequest.class));
        verify(userService).create(eq(orgId), any(UserCreateRequest.class));
    }
}
