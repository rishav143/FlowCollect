package com.flowcollect.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notification.email")
public class NotificationEmailProperties {

    /** From address for invoice/payment reminder emails sent to customers. */
    private String fromAddress;

    /** From address for account emails (verification, password reset) sent to org owners. */
    private String authFromAddress;

    private String fromName = "FlowCollect";

    public String getFromAddress() { return fromAddress; }
    public void setFromAddress(String fromAddress) { this.fromAddress = fromAddress; }

    public String getAuthFromAddress() { return authFromAddress; }
    public void setAuthFromAddress(String authFromAddress) { this.authFromAddress = authFromAddress; }

    public String getFromName() { return fromName; }
    public void setFromName(String fromName) { this.fromName = fromName; }
}
