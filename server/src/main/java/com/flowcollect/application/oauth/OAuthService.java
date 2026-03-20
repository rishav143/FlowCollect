package com.flowcollect.application.oauth;

import com.flowcollect.api.v1.auth.dto.LoginResponse;
import com.flowcollect.api.v1.organization.dto.OrganizationCreateRequest;
import com.flowcollect.application.auth.LoginResponseFactory;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.domain.oauth.OAuthProvider;
import com.flowcollect.domain.oauth.UserOAuthConnection;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.user.User;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.exception.http.UnauthorizedException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.oauth.UserOAuthConnectionJpaRepository;
import com.flowcollect.infrastructure.persistence.user.UserJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Orchestrates the OAuth 2.0 login and registration flows.
 *
 * <p><b>Login flow</b> ({@link OAuthMode#LOGIN}):
 * <ol>
 *   <li>Look up the {@link UserOAuthConnection} by provider + providerUserId.</li>
 *   <li>If absent: find the user by email in the target organization and link the connection.</li>
 *   <li>If still absent: reject — the user must register first.</li>
 *   <li>Sync the provider's latest profile image to the user record.</li>
 *   <li>Issue a JWT and return a {@link LoginResponse}.</li>
 * </ol>
 *
 * <p><b>Register flow</b> ({@link OAuthMode#REGISTER}):
 * <ol>
 *   <li>Create a new {@link Organization} (name derived from the OAuth profile).</li>
 *   <li>Create a new {@link User} with {@code ADMIN} role and no password.</li>
 *   <li>Create and persist the {@link UserOAuthConnection}.</li>
 *   <li>Issue a JWT and return a {@link LoginResponse}.</li>
 * </ol>
 */
@Service
public class OAuthService {

    private final Map<OAuthProvider, OAuthProviderClient> providerClients;
    private final OAuthStateService stateService;
    private final UserJpaRepository userRepository;
    private final UserOAuthConnectionJpaRepository oauthConnectionRepository;
    private final OrganizationService organizationService;
    private final LoginResponseFactory loginResponseFactory;

    public OAuthService(
            List<OAuthProviderClient> providerClients,
            OAuthStateService stateService,
            UserJpaRepository userRepository,
            UserOAuthConnectionJpaRepository oauthConnectionRepository,
            OrganizationService organizationService,
            LoginResponseFactory loginResponseFactory
    ) {
        this.providerClients = providerClients.stream()
                .collect(Collectors.toMap(OAuthProviderClient::getProvider, Function.identity()));
        this.stateService = stateService;
        this.userRepository = userRepository;
        this.oauthConnectionRepository = oauthConnectionRepository;
        this.organizationService = organizationService;
        this.loginResponseFactory = loginResponseFactory;
    }

    /**
     * Builds the authorization URL to redirect the browser to.
     *
     * @param provider    Google or Microsoft
     * @param mode        LOGIN (existing user) or REGISTER (new org + user)
     * @param redirectUri frontend callback URI registered with the provider
     */
    public String buildAuthorizationUrl(
            OAuthProvider provider,
            OAuthMode mode,
            String redirectUri
    ) {
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new ValidationException("redirectUri must not be blank");
        }

        String state = (mode == OAuthMode.LOGIN)
                ? stateService.createLoginState()
                : stateService.createRegisterState();

        return resolveClient(provider).buildAuthorizationUrl(redirectUri, state);
    }

    /**
     * Handles the OAuth callback: exchanges the code, resolves/creates the user, and returns a JWT.
     *
     * @param provider    Google or Microsoft
     * @param code        authorization code from the provider
     * @param state       the CSRF state token issued in {@link #buildAuthorizationUrl}
     * @param redirectUri must match the URI used when requesting the authorization URL
     */
    @Transactional
    public LoginResponse handleCallback(
            OAuthProvider provider,
            String code,
            String state,
            String redirectUri
    ) {
        if (code == null || code.isBlank()) {
            throw new ValidationException("Authorization code must not be blank");
        }
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new ValidationException("redirectUri must not be blank");
        }

        OAuthStateService.StateData stateData = stateService.parseState(state);
        OAuthUserProfile profile = resolveClient(provider).fetchUserProfile(code, redirectUri);

        User user = switch (stateData.mode()) {
            case LOGIN    -> handleLogin(provider, profile);
            case REGISTER -> handleRegister(provider, profile);
        };

        return loginResponseFactory.create(user);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private User handleLogin(OAuthProvider provider, OAuthUserProfile profile) {
        Optional<UserOAuthConnection> existingConnection =
                oauthConnectionRepository.findByProviderAndProviderUserId(provider, profile.providerUserId());

        User user;
        if (existingConnection.isPresent()) {
            user = existingConnection.get().getUser();
        } else {
            // First-time OAuth login: link this provider to the existing email-based account
            user = userRepository.findByEmail(profile.email())
                    .orElseThrow(() -> new UnauthorizedException(
                            "No account found for " + profile.email() + ". Please register first."));

            oauthConnectionRepository.save(new UserOAuthConnection(user, provider, profile.providerUserId()));
        }

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        syncProfileImage(user, profile.profileImageUrl());
        return user;
    }

    private User handleRegister(OAuthProvider provider, OAuthUserProfile profile) {
        // 1. Create organization (name derived from OAuth display name)
        OrganizationCreateRequest orgReq = new OrganizationCreateRequest();
        orgReq.setName(deriveOrganizationName(profile));
        orgReq.setEmail(profile.email());
        orgReq.setCurrency("USD");
        orgReq.setTimezone("UTC");

        Organization organization = organizationService.createForRegistration(orgReq);

        // 2. Create owner user (no password — OAuth-only account)
        User user = new User(organization, profile.name(), profile.email(), null, UserRole.ADMIN);
        if (profile.profileImageUrl() != null) {
            user.setProfileImageUrl(profile.profileImageUrl());
        }
        user = userRepository.save(user);

        // 3. Link the OAuth connection
        oauthConnectionRepository.save(new UserOAuthConnection(user, provider, profile.providerUserId()));

        return user;
    }

    /**
     * Updates the user's cached profile image URL if the provider returned a new one.
     * No-op when the URL is unchanged or the provider returned null.
     */
    private void syncProfileImage(User user, String newProfileImageUrl) {
        if (newProfileImageUrl == null) {
            return;
        }
        if (newProfileImageUrl.equals(user.getProfileImageUrl())) {
            return;
        }
        user.setProfileImageUrl(newProfileImageUrl);
        userRepository.save(user);
    }

    /**
     * Derives a sensible organization name from the OAuth profile.
     * Uses the display name if available, otherwise falls back to the email domain.
     */
    private String deriveOrganizationName(OAuthUserProfile profile) {
        if (profile.name() != null && !profile.name().isBlank()) {
            return profile.name().trim();
        }
        String email = profile.email();
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(atIndex + 1) : email;
    }

    private OAuthProviderClient resolveClient(OAuthProvider provider) {
        OAuthProviderClient client = providerClients.get(provider);
        if (client == null) {
            throw new ValidationException("Unsupported OAuth provider: " + provider);
        }
        return client;
    }
}
