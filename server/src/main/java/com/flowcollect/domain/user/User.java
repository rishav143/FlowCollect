package com.flowcollect.domain.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

import com.flowcollect.domain.organization.Organization;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_org_email", columnNames = {"organization_id", "email"})
        }
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    // Tenant Ownership

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    // Identity

    @NotBlank
    @Size(max = 100)
    private String name;

    @NotBlank
    @Email
    @Size(max = 100)
    @Column(nullable = false)
    private String email;

    // Security

    /** Null for OAuth-only accounts (created via social login with no password set). */
    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    // Lifecycle

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    // JPA

    protected User() {
        // JPA only
    }

    public User(Organization organization, String name, String email, String passwordHash, UserRole role) {
        this.organization = organization;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;  // null is valid for OAuth-only accounts
        this.role = role;
        this.status = UserStatus.ACTIVE;
    }

    // Callbacks

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters

    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Setters

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void deactivate() {
        this.status = UserStatus.INACTIVE;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }
}
