package com.flowcollect.security;

import java.util.UUID;

import com.flowcollect.domain.user.UserRole;

/**
 * Global context holder for the current authenticated user (ids + role).
 * Set by JwtFilter, cleared after request. Use in controllers/services and for @RequireRole.
 */
public final class AuthContext {

    private static final ThreadLocal<AuthContext> HOLDER = new ThreadLocal<>();

    private final UUID userId;
    private final UUID organizationId;
    private final UserRole role;

    public AuthContext(UUID userId, UUID organizationId, UserRole role) {
        this.userId = userId;
        this.organizationId = organizationId;
        this.role = role;
    }

    public static void set(AuthContext context) {
        HOLDER.set(context);
    }

    public static AuthContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UserRole getRole() {
        return role;
    }
}
