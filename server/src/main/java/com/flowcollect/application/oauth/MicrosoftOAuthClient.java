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
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * OAuth 2.0 client for Microsoft (Azure AD / Entra ID).
 *
 * Uses the v2.0 endpoint with scopes: openid email profile User.Read.
 * The tenant defaults to "common" (personal + work/school accounts).
 *
 * Profile photos from Microsoft Graph require an authenticated download;
 * there is no permanently public URL. Profile image is therefore not
 * populated — callers receive {@code null} for that field.
 */
@Component
public class MicrosoftOAuthClient implements OAuthProviderClient {

    private static final String AUTH_ENDPOINT =
            "https://login.microsoftonline.com/{tenant}/oauth2/v2.0/authorize";
    private static final String TOKEN_ENDPOINT =
            "https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token";
    private static final String USERINFO_ENDPOINT =
            "https://graph.microsoft.com/v1.0/me";
    private static final String OAUTH_SCOPES =
            "openid email profile User.Read";

    private final OAuthProperties.Provider config;
    private final RestClient restClient;

    public MicrosoftOAuthClient(OAuthProperties oAuthProperties, RestClient restClient) {
        this.config = oAuthProperties.getMicrosoft();
        this.restClient = restClient;
    }

    @Override
    public OAuthProvider getProvider() {
        return OAuthProvider.MICROSOFT;
    }

    @Override
    public String buildAuthorizationUrl(String redirectUri, String state) {
        String tenant = resolveTenant();
        String authUrl = AUTH_ENDPOINT.replace("{tenant}", tenant);
        return UriComponentsBuilder.fromUriString(authUrl)
                .queryParam("client_id", config.getClientId())
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", OAUTH_SCOPES)
                .queryParam("state", state)
                .queryParam("prompt", "select_account")
                .build()
                .toUriString();
    }

    @Override
    public OAuthUserProfile fetchUserProfile(String authorizationCode, String redirectUri) {
        String tokenUrl = TOKEN_ENDPOINT.replace("{tenant}", resolveTenant());
        TokenResponse tokenResponse = exchangeCodeForToken(authorizationCode, redirectUri, tokenUrl);
        return fetchUserInfo(tokenResponse.accessToken());
    }

    private TokenResponse exchangeCodeForToken(String code, String redirectUri, String tokenUrl) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", code);
        params.add("client_id", config.getClientId());
        params.add("client_secret", config.getClientSecret());
        params.add("redirect_uri", redirectUri);
        params.add("grant_type", "authorization_code");
        params.add("scope", OAUTH_SCOPES);

        try {
            TokenResponse response = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(params)
                    .retrieve()
                    .body(TokenResponse.class);

            if (response == null || response.accessToken() == null) {
                throw new UnauthorizedException("Microsoft did not return an access token");
            }
            return response;
        } catch (UnauthorizedException e) {
            throw e;
        } catch (RestClientException e) {
            throw new UnauthorizedException("Failed to exchange Microsoft authorization code: " + e.getMessage());
        }
    }

    private OAuthUserProfile fetchUserInfo(String accessToken) {
        try {
            MicrosoftUserInfo userInfo = restClient.get()
                    .uri(USERINFO_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(MicrosoftUserInfo.class);

            if (userInfo == null || userInfo.id() == null) {
                throw new UnauthorizedException("Could not retrieve user profile from Microsoft");
            }

            String email = resolveEmail(userInfo);

            return new OAuthUserProfile(
                    userInfo.id(),
                    email.trim().toLowerCase(),
                    userInfo.displayName(),
                    null   // Microsoft photos require authenticated download — not a public URL
            );
        } catch (UnauthorizedException e) {
            throw e;
        } catch (RestClientException e) {
            throw new UnauthorizedException("Failed to fetch user profile from Microsoft: " + e.getMessage());
        }
    }

    /**
     * Microsoft accounts may store the email in {@code mail} or fall back to
     * {@code userPrincipalName} (which contains "@" for non-guest accounts).
     */
    private String resolveEmail(MicrosoftUserInfo userInfo) {
        if (StringUtils.hasText(userInfo.mail())) {
            return userInfo.mail();
        }
        String upn = userInfo.userPrincipalName();
        if (StringUtils.hasText(upn) && upn.contains("@")) {
            return upn;
        }
        throw new UnauthorizedException(
                "Microsoft account does not expose a usable email address. "
                + "Ensure the account has a verified email and User.Read permission is granted.");
    }

    private String resolveTenant() {
        return StringUtils.hasText(config.getTenant()) ? config.getTenant() : "common";
    }

    // ── Internal DTOs ────────────────────────────────────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type")   String tokenType
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record MicrosoftUserInfo(
            String id,
            String displayName,
            String mail,
            String userPrincipalName
    ) {}
}
