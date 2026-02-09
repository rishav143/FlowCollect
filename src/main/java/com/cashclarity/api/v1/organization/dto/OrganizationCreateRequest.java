package com.cashclarity.api.v1.organization.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class OrganizationCreateRequest {

    @NotBlank(message = "Organization name is required and cannot be blank.")
    @Size(max = 100, message = "Organization name must not exceed 100 characters.")
    private String name;

    @NotBlank(message = "Email is required and cannot be blank.")
    @Email(message = "Email must be a valid email address.")
    @Size(max = 100, message = "Email must not exceed 100 characters.")
    private String email;

    @Size(max = 20, message = "Phone must not exceed 20 characters.")
    private String phone;

    @Size(max = 255, message = "Address must not exceed 255 characters.")
    private String address;

    @NotBlank(message = "Currency is required. Provide a valid ISO 4217 code (e.g. USD, EUR, INR).")
    @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO 4217 code.")
    private String currency;

    @NotBlank(message = "Timezone is required. Provide a valid timezone ID (e.g. America/New_York, Europe/London).")
    @Size(max = 50, message = "Timezone must not exceed 50 characters.")
    private String timezone;

    /* ======================
       Getters & Setters
       ====================== */

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }
}
