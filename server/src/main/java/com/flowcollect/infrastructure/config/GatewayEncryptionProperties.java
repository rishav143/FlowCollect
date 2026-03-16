package com.flowcollect.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Platform-level AES-256 key used to encrypt Razorpay API credentials at rest.
 *
 * Required env var: GATEWAY_ENCRYPTION_KEY
 * Must be exactly 32 characters (256 bits) when used as AES key material.
 *
 * This key protects ALL stored Razorpay credentials across all organizations.
 * Rotate it carefully — existing encrypted values must be re-encrypted.
 */
@Component
@ConfigurationProperties(prefix = "gateway.encryption")
public class GatewayEncryptionProperties {

    private String key;

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
}
