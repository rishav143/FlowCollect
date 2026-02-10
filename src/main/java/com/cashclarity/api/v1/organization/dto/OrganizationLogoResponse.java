package com.cashclarity.api.v1.organization.dto;

public class OrganizationLogoResponse {

    private Long id;
    private String logoUrl;

    public OrganizationLogoResponse() {
    }

    public OrganizationLogoResponse(Long id, String logoUrl) {
        this.id = id;
        this.logoUrl = logoUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }
}
