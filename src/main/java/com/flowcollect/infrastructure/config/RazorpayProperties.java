package com.flowcollect.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Razorpay gateway configuration.
 *
 * Properties prefix: razorpay
 * Required env vars (when razorpay.enabled=true):
 *   RAZORPAY_KEY_ID         - Razorpay Key ID (rzp_live_... or rzp_test_...)
 *   RAZORPAY_KEY_SECRET     - Razorpay Key Secret
 *   RAZORPAY_WEBHOOK_SECRET - Secret configured in the Razorpay webhook dashboard
 */
@Component
@ConfigurationProperties(prefix = "razorpay")
public class RazorpayProperties {

    private boolean enabled = false;
    private String keyId;
    private String keySecret;
    private String webhookSecret;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }

    public String getKeySecret() { return keySecret; }
    public void setKeySecret(String keySecret) { this.keySecret = keySecret; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
}
