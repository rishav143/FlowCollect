package com.flowcollect.api.v1.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AcceptInviteRequest {

    @NotBlank(message = "Name must not be blank")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @NotBlank(message = "Password must not be blank")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    private String password;

    public String getName()     { return name; }
    public void setName(String name) { this.name = name; }

    public String getPassword()          { return password; }
    public void setPassword(String password) { this.password = password; }
}
