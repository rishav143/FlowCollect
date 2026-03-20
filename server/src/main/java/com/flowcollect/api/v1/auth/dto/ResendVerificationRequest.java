package com.flowcollect.api.v1.auth.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotNull;

public class ResendVerificationRequest {

    @NotNull(message = "organizationId is required")
    private UUID organizationId;

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
}
