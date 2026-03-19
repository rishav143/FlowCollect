package com.flowcollect.application.auth;

import com.flowcollect.api.v1.auth.dto.LoginResponse;
import com.flowcollect.domain.user.User;
import com.flowcollect.security.JwtService;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Builds a {@link LoginResponse} from an authenticated {@link User}.
 * Extracted to avoid duplicating token-building logic across
 * {@link AuthService} and the OAuth service layer.
 */
@Component
public class LoginResponseFactory {

    private final JwtService jwtService;

    public LoginResponseFactory(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    public LoginResponse create(User user) {
        String token = jwtService.createToken(
                user.getId(),
                user.getOrganization().getId(),
                user.getRole()
        );
        Instant expiresAt = Instant.now().plusMillis(jwtService.getExpirationMs());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setType("Bearer");
        response.setExpiresAt(expiresAt);
        response.setId(user.getId());
        response.setOrganizationId(user.getOrganization().getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus());
        response.setProfileImageUrl(user.getProfileImageUrl());
        return response;
    }
}
