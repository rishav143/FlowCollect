package com.cashclarity.domain.reminder;

import com.cashclarity.domain.organization.Organization;
import com.cashclarity.domain.template.Template;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Entity
@Table(
        name = "reminder_rules",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_reminder_rule_name_org",
                        columnNames = {"name", "organization_id"}
                )
        }
)
public class ReminderRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
    @Column(nullable = false)
    private String name;

    /* ======================
       Trigger Logic
       ====================== */

    /**
     * Days relative to due date:
     *  -2 → 2 days before due date
     *   0 → on due date
     *  +3 → 3 days after due date
     */
    @Column(name = "days_offset", nullable = false)
    private int daysOffset;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderTriggerType triggerType;

    /* ======================
       Communication
       ====================== */

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderChannel channel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private Template template;

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

    protected ReminderRule() {
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

    public Long getId() {
        return id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public String getName() {
        return name;
    }

    public int getDaysOffset() {
        return daysOffset;
    }

    public ReminderTriggerType getTriggerType() {
        return triggerType;
    }

    public ReminderChannel getChannel() {
        return channel;
    }

    public Template getTemplate() {
        return template;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
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

    public void setDaysOffset(int daysOffset) {
        this.daysOffset = daysOffset;
    }

    public void setTriggerType(ReminderTriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public void setChannel(ReminderChannel channel) {
        this.channel = channel;
    }

    public void setTemplate(Template template) {
        this.template = template;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
