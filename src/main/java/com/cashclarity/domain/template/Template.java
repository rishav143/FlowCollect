package com.cashclarity.domain.template;

import com.cashclarity.domain.organization.Organization;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "templates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_template_name_org",
                        columnNames = {"name", "organization_id"}
                )
        }
)
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /* ======================
       Ownership
       ====================== */

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /* ======================
       Identity
       ====================== */

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String name;

    /* ======================
       Channel
       ====================== */

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateChannel channel;

    /* ======================
       Content
       ====================== */

    @Size(max = 200)
    private String subject; // email only

    @NotBlank
    @Column(nullable = false)
    private String body;

    /* ======================
       Tone (optional intelligence)
       ====================== */

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateTone tone;

    /* ======================
       Lifecycle
       ====================== */

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    /* ======================
       JPA
       ====================== */

    public Template() {
        // JPA only
    }

    /* ======================
       Callbacks
       ====================== */

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /* ======================
       Getters
       ====================== */

    public UUID getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getName() {
        return name;
    }

    public TemplateChannel getChannel() {
        return channel;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public TemplateTone getTone() {
        return tone;
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
    /* ======================
       Setters
       ====================== */

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setChannel(TemplateChannel channel) {
        this.channel = channel;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setTone(TemplateTone tone) {
        this.tone = tone;
    }

    public void setActive() {
        this.active = true;
    }

    public void setDisabled() {
        this.active = false;
    }
}
