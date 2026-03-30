package com.flowcollect.api.v1.ai.dto;

public class AiInsightResponse {

    private String insights;

    public AiInsightResponse(String insights) {
        this.insights = insights;
    }

    public String getInsights() {
        return insights;
    }
}
