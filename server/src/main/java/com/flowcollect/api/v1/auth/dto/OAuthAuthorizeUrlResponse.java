package com.flowcollect.api.v1.auth.dto;

/**
 * Response body for the "get OAuth authorization URL" endpoint.
 * The client should redirect the browser to {@code authorizationUrl}.
 */
public class OAuthAuthorizeUrlResponse {

    private String authorizationUrl;

    public OAuthAuthorizeUrlResponse(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    public void setAuthorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
    }
}
