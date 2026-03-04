package com.paidpeace.api.v1.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserPasswordChangeRequest {

    @NotBlank(message = "newPassword must not be blank")
    @Size(min = 8, max = 100, message = "newPassword must be between 8 and 100 characters")
    private String newPassword;

    private String oldPassword;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }
}
