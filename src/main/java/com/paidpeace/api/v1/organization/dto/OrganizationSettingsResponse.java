package com.paidpeace.api.v1.organization.dto;

import java.util.UUID;

public class OrganizationSettingsResponse {

    private UUID id;
    private String timezone;
    private String currency;

    public OrganizationSettingsResponse() {
    }

    public OrganizationSettingsResponse(UUID id, String timezone, String currency) {
        this.id = id;
        this.timezone = timezone;
        this.currency = currency;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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
