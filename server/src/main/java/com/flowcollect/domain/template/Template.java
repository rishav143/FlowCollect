package com.flowcollect.domain.template;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.reminder.RuleMode;

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

    // Ownership — null for system-defined templates that belong to no specific org
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "organization_id", nullable = true)
    private Organization organization;

    // Mode — AUTO templates are used by the recovery engine; MANUAL are user-triggered only
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RuleMode mode = RuleMode.MANUAL;

    // True for product-defined templates seeded by the platform (shown to users but not deletable)
    @Column(name = "system_defined", nullable = false)
    private boolean systemDefined = false;

    // Identity
    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String name;

    // Channel
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TemplateChannel channel;

    // Content
    @Size(max = 200)
    private String subject; // email only

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    // Tone (optional intelligence)
    @NotNull
    @Enumerated(EnumType.STRING)
    private TemplateTone tone;

    // Lifecycle
    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    // JPA

    public Template() {
        // JPA only
    }

    // Callbacks

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters

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
    // Setters

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

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public RuleMode getMode() {
        return mode;
    }

    public void setMode(RuleMode mode) {
        this.mode = mode;
    }

    public boolean isSystemDefined() {
        return systemDefined;
    }

    public void setSystemDefined(boolean systemDefined) {
        this.systemDefined = systemDefined;
    }
}
