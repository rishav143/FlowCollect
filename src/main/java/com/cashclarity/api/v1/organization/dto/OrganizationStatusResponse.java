package com.cashclarity.api.v1.organization.dto;

import com.cashclarity.domain.organization.OrganizationStatus;

public class OrganizationStatusResponse {

    private Long id;
    private OrganizationStatus status;

    public OrganizationStatusResponse() {
    }

    public OrganizationStatusResponse(Long id, OrganizationStatus status) {
        this.id = id;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public OrganizationStatus getStatus() {
        return status;
    }

    public void setStatus(OrganizationStatus status) {
        this.status = status;
    }
}
