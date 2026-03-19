package com.flowcollect.domain.oauth;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

import com.flowcollect.domain.user.User;

/**
 * Links a User account to an external OAuth provider identity.
 * A user may have at most one connection per provider.
 */
@Entity
@Table(
        name = "user_oauth_connections",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_oauth_provider_provider_user_id",
                        columnNames = {"provider", "provider_user_id"}
                )
        }
)
public class UserOAuthConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuthProvider provider;

    @NotBlank
    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    // JPA only
    protected UserOAuthConnection() {}

    public UserOAuthConnection(User user, OAuthProvider provider, String providerUserId) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }

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

    public User getUser() {
        return user;
    }

    public OAuthProvider getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
