package com.flowcollect.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Stripe gateway configuration.
 *
 * Properties prefix: stripe
 * Required env vars (when stripe.enabled=true):
 *   STRIPE_API_KEY           - Stripe secret key (sk_live_... or sk_test_...)
 *   STRIPE_WEBHOOK_SECRET    - Signing secret from the Stripe webhook dashboard
 */
@Component
@ConfigurationProperties(prefix = "stripe")
public class StripeProperties {

    private boolean enabled = false;
    private String apiKey;
    private String webhookSecret;

    /**
     * Stripe Connect platform client ID (ca_xxxx) from the Stripe Dashboard.
     * Required for the Connect OAuth flow so organizations can link their own Stripe accounts.
     */
    private String connectClientId;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public String getConnectClientId() { return connectClientId; }
    public void setConnectClientId(String connectClientId) { this.connectClientId = connectClientId; }
}
