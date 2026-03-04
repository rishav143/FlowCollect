package com.paidpeace.api.v1.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class UserCreateRequest {

    @NotBlank(message = "name must not be blank")
    @Size(max = 100, message = "name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be a valid email address")
    @Size(max = 100, message = "email must not exceed 100 characters")
    private String email;

    @NotBlank(message = "password must not be blank")
    @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters")
    private String password;

    @NotNull(message = "role is required")
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
