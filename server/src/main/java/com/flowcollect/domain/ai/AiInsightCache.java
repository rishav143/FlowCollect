package com.flowcollect.domain.ai;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.flowcollect.domain.organization.Organization;

/**
 * Caches the AI overview insight for an organization.
 * One row per org — upserted on each (re)generation.
 * Stale when generatedForDate != today in the org's timezone.
 */
@Entity
@Table(name = "ai_insight_cache")
public class AiInsightCache {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String insights;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

    // Date of generation in the org's timezone — used for staleness check
    @Column(name = "generated_for_date", nullable = false)
    private LocalDate generatedForDate;

    // JPA
    public AiInsightCache() {}

    public AiInsightCache(Organization organization, String insights, LocalDate generatedForDate) {
        this.organization = organization;
        this.insights = insights;
        this.generatedAt = Instant.now();
        this.generatedForDate = generatedForDate;
    }

    // Overwrite with fresh data (reuse same row)
    public void refresh(String insights, LocalDate generatedForDate) {
        this.insights = insights;
        this.generatedAt = Instant.now();
        this.generatedForDate = generatedForDate;
    }

    public UUID getId() { return id; }
    public Organization getOrganization() { return organization; }
    public String getInsights() { return insights; }
    public Instant getGeneratedAt() { return generatedAt; }
    public LocalDate getGeneratedForDate() { return generatedForDate; }
}
