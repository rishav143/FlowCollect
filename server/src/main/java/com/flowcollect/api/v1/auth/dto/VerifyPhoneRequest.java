package com.flowcollect.api.v1.auth.dto;

import java.util.UUID;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class VerifyPhoneRequest {

    @NotNull(message = "organizationId is required")
    private UUID organizationId;

    @NotBlank(message = "OTP is required")
    private String otp;

    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}
