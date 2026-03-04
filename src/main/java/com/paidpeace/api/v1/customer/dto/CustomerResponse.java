package com.paidpeace.api.v1.customer.dto;

import java.time.Instant;
import java.util.UUID;

public class CustomerResponse {
    private UUID id;
    private String name;
    private UUID organizationId;
    private String email;
    private String phone;
    private String address;
    private String companyName;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    // Getters

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }
    
    public String getCompanyName() {
        return companyName;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Setters

    public void setId(UUID id) {
        this.id = id;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }

    public void setAddress(String address) {
        this.address = address;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}