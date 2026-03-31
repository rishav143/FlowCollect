package com.flowcollect.api.v1.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class OrganizationUpdateRequest {

    @Size(max = 100)
    private String name;

    @Email
    @Size(max = 100)
    private String email;

    @Size(max = 20)
    private String phone;

    @Size(max = 255)
    private String address;

    @Size(max = 255)
    private String logoUrl;

    @Size(min = 3, max = 3)
    private String currency;

    @Size(max = 50)
    private String timezone;

    /** PAYMENT_LINK or CONFIRMATION_FLOW. Null means no change. */
    private String paymentCollectionMode;

    /** Null means no change. */
    private Boolean autoRecoveryEnabled;

    /* ======================
       Getters & Setters
       ====================== */

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getPaymentCollectionMode() {
        return paymentCollectionMode;
    }

    public void setPaymentCollectionMode(String paymentCollectionMode) {
        this.paymentCollectionMode = paymentCollectionMode;
    }

    public Boolean getAutoRecoveryEnabled() { return autoRecoveryEnabled; }
    public void setAutoRecoveryEnabled(Boolean autoRecoveryEnabled) { this.autoRecoveryEnabled = autoRecoveryEnabled; }
}
