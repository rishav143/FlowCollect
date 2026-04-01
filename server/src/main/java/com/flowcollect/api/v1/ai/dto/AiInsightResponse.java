package com.flowcollect.api.v1.ai.dto;

import java.time.Instant;

public class AiInsightResponse {

    private String insights;
    private boolean cached;
    private Instant generatedAt;

    public AiInsightResponse(String insights, boolean cached, Instant generatedAt) {
        this.insights = insights;
        this.cached = cached;
        this.generatedAt = generatedAt;
    }

    public String getInsights() { return insights; }
    public boolean isCached() { return cached; }
    public Instant getGeneratedAt() { return generatedAt; }
}
