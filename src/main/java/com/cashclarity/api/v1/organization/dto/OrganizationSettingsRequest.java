package com.cashclarity.api.v1.organization.dto;

public class OrganizationSettingsRequest {

    private String timezone;
    private String currency;

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
