package com.flowcollect.domain.reminder;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.template.Template;

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
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Ownership
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    // Identity
    @NotBlank
    @Column(nullable = false)
    private String name;

    // Trigger Logic

    //  Days relative to due date:
    //  -2 → 2 days before due date
    //   0 → on due date
    //  +3 → 3 days after due date
    @Column(name = "days_offset", nullable = false)
    private int daysOffset;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderTriggerType triggerType;

    // Communication
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReminderChannel channel;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @Column(name = "attach_pdf", nullable = false)
    private boolean attachPdf = false;

    // Cycle & Recurrence

    // How many times this rule fires per invoice. Minimum 1.
    @Column(name = "max_occurrences", nullable = false)
    private int maxOccurrences = 1;

    // Interval in days between consecutive occurrences.
    // Must be >= 1 when maxOccurrences > 1; ignored (0) when maxOccurrences == 1.
    @Column(name = "cycle_interval_days", nullable = false)
    private int cycleIntervalDays = 0;

    // Per-occurrence template overrides (ordered by occurrence index).
    // Empty list means all occurrences use the default `template` above.
    // When non-empty, size must equal maxOccurrences.
    @ElementCollection
    @CollectionTable(
        name = "reminder_rule_occurrence_templates",
        joinColumns = @JoinColumn(name = "reminder_rule_id")
    )
    @Column(name = "template_id", nullable = false)
    @OrderColumn(name = "occurrence_index")
    private List<UUID> occurrenceTemplateIds = new ArrayList<>();

    // Optional date on or after which the automation engine begins processing this rule.
    // Null means no restriction — the engine starts immediately.
    @Column(name = "start_date")
    private LocalDate startDate;

    // Lifecycle
    @Column(nullable = false)
    private boolean active = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    // JPA
    public ReminderRule() {
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

    public boolean isAttachPdf() {
        return attachPdf;
    }

    public int getMaxOccurrences() {
        return maxOccurrences;
    }

    public int getCycleIntervalDays() {
        return cycleIntervalDays;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public List<UUID> getOccurrenceTemplateIds() {
        return occurrenceTemplateIds;
    }

    public void setOccurrenceTemplateIds(List<UUID> occurrenceTemplateIds) {
        this.occurrenceTemplateIds = occurrenceTemplateIds != null ? occurrenceTemplateIds : new ArrayList<>();
    }

    public boolean isCyclic() {
        return maxOccurrences > 1;
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

    // Setters
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

    public void setAttachPdf(boolean attachPdf) {
        this.attachPdf = attachPdf;
    }

    public void setMaxOccurrences(int maxOccurrences) {
        this.maxOccurrences = maxOccurrences;
    }

    public void setCycleIntervalDays(int cycleIntervalDays) {
        this.cycleIntervalDays = cycleIntervalDays;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
