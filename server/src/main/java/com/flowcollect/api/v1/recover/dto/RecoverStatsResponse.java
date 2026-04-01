package com.flowcollect.api.v1.recover.dto;

import java.math.BigDecimal;

public class RecoverStatsResponse {

    private final BigDecimal totalRecovered;
    private final long activeRules;
    private final long sentToday;
    private final long pendingToday;
    private final long sentThisWeek;

    public RecoverStatsResponse(BigDecimal totalRecovered, long activeRules, long sentToday, long pendingToday, long sentThisWeek) {
        this.totalRecovered = totalRecovered;
        this.activeRules    = activeRules;
        this.sentToday      = sentToday;
        this.pendingToday   = pendingToday;
        this.sentThisWeek   = sentThisWeek;
    }

    public BigDecimal getTotalRecovered() { return totalRecovered; }
    public long getActiveRules()          { return activeRules; }
    public long getSentToday()            { return sentToday; }
    public long getPendingToday()         { return pendingToday; }
    public long getSentThisWeek()         { return sentThisWeek; }
}
