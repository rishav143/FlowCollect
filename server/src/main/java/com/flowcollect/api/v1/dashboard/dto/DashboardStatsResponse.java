package com.flowcollect.api.v1.dashboard.dto;

import java.util.List;

public class DashboardStatsResponse {

    private final List<NeedsAttentionItem> needsAttention;

    public DashboardStatsResponse(List<NeedsAttentionItem> needsAttention) {
        this.needsAttention = needsAttention;
    }

    public List<NeedsAttentionItem> getNeedsAttention() {
        return needsAttention;
    }
}
