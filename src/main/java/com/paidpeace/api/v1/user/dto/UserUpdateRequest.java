package com.paidpeace.api.v1.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public class UserUpdateRequest {

    @Size(max = 100, message = "name must not exceed 100 characters")
    private String name;

    @Email(message = "email must be a valid email address")
    @Size(min = 8, max = 16, message = "email must be between 8 and 16 characters")
    private String email;

    private String role;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
