package com.cashclarity.api.v1.organization.dto;

public class OrganizationSettingsResponse {

    private Long id;
    private String timezone;
    private String currency;

    public OrganizationSettingsResponse() {
    }

    public OrganizationSettingsResponse(Long id, String timezone, String currency) {
        this.id = id;
        this.timezone = timezone;
        this.currency = currency;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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
