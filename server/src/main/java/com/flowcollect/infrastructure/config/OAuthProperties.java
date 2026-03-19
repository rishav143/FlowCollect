package com.flowcollect.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OAuth 2.0 provider credentials.
 *
 * Configure via environment variables:
 *   GOOGLE_OAUTH_CLIENT_ID / GOOGLE_OAUTH_CLIENT_SECRET
 *   MICROSOFT_OAUTH_CLIENT_ID / MICROSOFT_OAUTH_CLIENT_SECRET
 *   MICROSOFT_OAUTH_TENANT  (optional, defaults to "common" for multi-tenant)
 */
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties {

    private Provider google = new Provider();
    private Provider microsoft = new Provider();

    public Provider getGoogle() {
        return google;
    }

    public void setGoogle(Provider google) {
        this.google = google;
    }

    public Provider getMicrosoft() {
        return microsoft;
    }

    public void setMicrosoft(Provider microsoft) {
        this.microsoft = microsoft;
    }

    public static class Provider {

        private String clientId = "";
        private String clientSecret = "";

        /**
         * Microsoft only: the Azure AD tenant ID or "common" for any tenant.
         * Ignored for Google.
         */
        private String tenant = "common";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getTenant() {
            return tenant;
        }

        public void setTenant(String tenant) {
            this.tenant = tenant;
        }
    }
}
