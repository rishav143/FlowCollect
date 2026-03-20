package com.flowcollect.domain.verification;

import java.time.Instant;
import java.util.UUID;

import com.flowcollect.domain.organization.Organization;
import jakarta.persistence.*;

@Entity
@Table(name = "phone_otps")
public class PhoneOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String otp;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected PhoneOtp() {}

    public PhoneOtp(Organization organization, String phone, String otp, Instant expiresAt) {
        this.organization = organization;
        this.phone = phone;
        this.otp = otp;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Organization getOrganization() { return organization; }
    public String getPhone() { return phone; }
    public String getOtp() { return otp; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isUsed() { return used; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void markUsed() {
        this.used = true;
    }
}
