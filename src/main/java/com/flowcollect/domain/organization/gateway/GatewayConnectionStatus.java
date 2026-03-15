package com.flowcollect.domain.organization.gateway;

/**
 * Lifecycle status of an organization's payment gateway connection.
 *
 * CONNECTED    - credentials are stored and the gateway is usable for payment links.
 * DISCONNECTED - the org has revoked access or an admin disconnected it.
 */
public enum GatewayConnectionStatus {
    CONNECTED,
    DISCONNECTED
}
