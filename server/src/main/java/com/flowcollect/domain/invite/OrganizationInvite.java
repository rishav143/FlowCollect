package com.flowcollect.domain.invite;

import java.time.Instant;
import java.util.UUID;

import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.user.User;
import com.flowcollect.domain.user.UserRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Represents a pending invitation for someone to join an organization.
 *
 * <p>Lifecycle: PENDING → ACCEPTED (invitee accepts) or REVOKED (admin cancels).
 * A new User record is created only when the invite is accepted — never before.
 *
 * <p>One PENDING invite per (organization + email) is enforced in the service layer.
 * Multiple REVOKED or ACCEPTED records for the same email are allowed (history).
 */
@Entity
@Table(name = "organization_invites")
public class OrganizationInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /** Email address of the person being invited. */
    @Column(nullable = false, length = 100)
    private String email;

    /** Role the invitee will receive when they accept. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    /** Secure random 32-char hex token embedded in the invite URL. */
    @Column(nullable = false, unique = true, length = 64)
    private String token;

    /**
     * The user who sent the invitation.
     * Nullable because the inviter's account may be deleted before the invite is accepted.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by_id")
    private User invitedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InviteStatus status = InviteStatus.PENDING;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected OrganizationInvite() {}

    public OrganizationInvite(Organization organization, String email, UserRole role,
                               String token, User invitedBy, Instant expiresAt) {
        this.organization = organization;
        this.email        = email;
        this.role         = role;
        this.token        = token;
        this.invitedBy    = invitedBy;
        this.expiresAt    = expiresAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Domain behaviour

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void accept() {
        this.status = InviteStatus.ACCEPTED;
    }

    public void revoke() {
        this.status = InviteStatus.REVOKED;
    }

    // Getters

    public UUID getId()                    { return id; }
    public Organization getOrganization()  { return organization; }
    public String getEmail()               { return email; }
    public UserRole getRole()              { return role; }
    public String getToken()               { return token; }
    public User getInvitedBy()             { return invitedBy; }
    public InviteStatus getStatus()        { return status; }
    public Instant getExpiresAt()          { return expiresAt; }
    public Instant getCreatedAt()          { return createdAt; }
}
