package com.flowcollect.application.oauth;

/**
 * Normalised user profile returned by any OAuth provider.
 * {@code profileImageUrl} may be {@code null} when the provider does not supply one.
 */
public record OAuthUserProfile(
        String providerUserId,
        String email,
        String name,
        String profileImageUrl
) {}
