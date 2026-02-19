package com.cashclarity.api.v1.organization.dto;

import com.cashclarity.domain.organization.OrganizationStatus;
import java.util.UUID;

public class OrganizationStatusResponse {

    private UUID id;
    private OrganizationStatus status;

    public OrganizationStatusResponse() {
    }

    public OrganizationStatusResponse(UUID id, OrganizationStatus status) {
        this.id = id;
        this.status = status;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public OrganizationStatus getStatus() {
        return status;
    }

    public void setStatus(OrganizationStatus status) {
        this.status = status;
    }
}
