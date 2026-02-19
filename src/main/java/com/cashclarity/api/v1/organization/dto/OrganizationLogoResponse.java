package com.cashclarity.api.v1.organization.dto;

import java.util.UUID;

public class OrganizationLogoResponse {

    private UUID id;
    private String logoUrl;

    public OrganizationLogoResponse() {
    }

    public OrganizationLogoResponse(UUID id, String logoUrl) {
        this.id = id;
        this.logoUrl = logoUrl;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }
}
