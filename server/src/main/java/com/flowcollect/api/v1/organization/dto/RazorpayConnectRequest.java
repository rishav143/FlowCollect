package com.flowcollect.api.v1.organization.dto;

import jakarta.validation.constraints.NotBlank;

/** Request body for manually connecting a Razorpay account. */
public class RazorpayConnectRequest {

    @NotBlank(message = "Razorpay Key ID is required")
    private String keyId;

    @NotBlank(message = "Razorpay Key Secret is required")
    private String keySecret;

    @NotBlank(message = "Razorpay webhook secret is required")
    private String webhookSecret;

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }

    public String getKeySecret() { return keySecret; }
    public void setKeySecret(String keySecret) { this.keySecret = keySecret; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }
}
