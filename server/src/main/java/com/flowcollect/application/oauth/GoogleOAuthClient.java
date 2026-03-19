package com.flowcollect.application.oauth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.flowcollect.domain.oauth.OAuthProvider;
import com.flowcollect.exception.http.UnauthorizedException;
import com.flowcollect.infrastructure.config.OAuthProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * OAuth 2.0 client for Google (OpenID Connect).
 *
 * Scopes requested: openid email profile
 * Profile photo: returned as a direct public URL in the {@code picture} claim.
 */
@Component
public class GoogleOAuthClient implements OAuthProviderClient {

    private static final String AUTH_ENDPOINT =
            "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT =
            "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT =
            "https://www.googleapis.com/oauth2/v3/userinfo";

    private final OAuthProperties.Provider config;
    private final RestClient restClient;

    public GoogleOAuthClient(OAuthProperties oAuthProperties, RestClient restClient) {
        this.config = oAuthProperties.getGoogle();
        this.restClient = restClient;
    }

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.GOOGLE;
    }

    @Override
    public String buildAuthorizationUrl(String redirectUri, String state) {
        return UriComponentsBuilder.fromUriString(AUTH_ENDPOINT)
                .queryParam("client_id", config.getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "select_account")
                .build()
                .toUriString();
    }

    @Override
    public OAuthUserProfile fetchUserProfile(String authorizationCode, String redirectUri) {
        TokenResponse tokenResponse = exchangeCodeForToken(authorizationCode, redirectUri);
        return fetchUserInfo(tokenResponse.accessToken());
    }

    private TokenResponse exchangeCodeForToken(String code, String redirectUri) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", config.getClientId());
        params.add("client_secret", config.getClientSecret());
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");

        try {
            TokenResponse response = restClient.post()
                    .uri(TOKEN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(TokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new UnauthorizedException("Google did not return an access token");
            }
            return response;
        } catch (UnauthorizedException e) {
            throw e;
        } catch (RestClientException e) {
            throw new UnauthorizedException("Failed to exchange Google authorization code: " + e.getMessage());
        }
    }

    private OAuthUserProfile fetchUserInfo(String accessToken) {
        try {
            GoogleUserInfo userInfo = restClient.get()
                    .uri(USERINFO_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(GoogleUserInfo.class);

            if (userInfo == null || userInfo.sub() == null || userInfo.email() == null) {
                throw new UnauthorizedException("Could not retrieve user profile from Google");
            }
            if (!Boolean.TRUE.equals(userInfo.emailVerified())) {
                throw new UnauthorizedException("Google account email is not verified");
            }

            return new OAuthUserProfile(
                    userInfo.sub(),
                    userInfo.email().trim().toLowerCase(),
                    userInfo.name(),
                    userInfo.picture()
            );
        } catch (UnauthorizedException e) {
            throw e;
        } catch (RestClientException e) {
            throw new UnauthorizedException("Failed to fetch user profile from Google: " + e.getMessage());
        }
    }

    // ── Internal DTOs ────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type")   String tokenType
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GoogleUserInfo(
            String sub,
            String email,
            @JsonProperty("email_verified") Boolean emailVerified,
            String name,
            String picture
    ) {}
}
