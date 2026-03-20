package com.flowcollect.api.v1.auth.dto;

import java.util.UUID;

public class RegisterResponse {

    private UUID organizationId;
    private UUID userId;
    private boolean emailVerificationRequired;
    private boolean phoneVerificationRequired;
    private String message;

    public RegisterResponse(UUID organizationId, UUID userId,
                            boolean emailVerificationRequired,
                            boolean phoneVerificationRequired) {
        this.organizationId = organizationId;
        this.userId = userId;
        this.emailVerificationRequired = emailVerificationRequired;
        this.phoneVerificationRequired = phoneVerificationRequired;
        this.message = buildMessage(emailVerificationRequired, phoneVerificationRequired);
    }

    private static String buildMessage(boolean emailRequired, boolean phoneRequired) {
        if (emailRequired && phoneRequired) {
            return "Please verify your email address and phone number to activate your account";
        } else if (emailRequired) {
            return "Please check your email and verify your address to activate your account";
        } else if (phoneRequired) {
            return "Please enter the OTP sent to your phone number";
        }
        return "Registration successful";
    }

    public UUID getOrganizationId() { return organizationId; }
    public UUID getUserId() { return userId; }
    public boolean isEmailVerificationRequired() { return emailVerificationRequired; }
    public boolean isPhoneVerificationRequired() { return phoneVerificationRequired; }
    public String getMessage() { return message; }
}
