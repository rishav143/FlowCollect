package com.flowcollect.application.oauth;

import com.flowcollect.domain.oauth.OAuthProvider;

/**
 * Strategy interface for interacting with a specific OAuth 2.0 provider.
 * Each implementation handles one provider (Google, Microsoft, …).
 */
public interface OAuthProviderClient {

    /** The provider this client handles. */
    OAuthProvider getProvider();

    /**
     * Builds the provider's authorization URL to redirect the user to.
     *
     * @param redirectUri the URI the provider will redirect back to after user consent
     * @param state       CSRF-protection token (created by {@link OAuthStateService})
     */
    String buildAuthorizationUrl(String redirectUri, String state);

    /**
     * Exchanges an authorization code for an access token and fetches the user's profile.
     *
     * @param authorizationCode the one-time code returned by the provider
     * @param redirectUri       must exactly match the URI used in the authorization request
     */
    OAuthUserProfile fetchUserProfile(String authorizationCode, String redirectUri);
}
