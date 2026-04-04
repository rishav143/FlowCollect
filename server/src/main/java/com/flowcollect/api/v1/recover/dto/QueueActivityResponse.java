package com.flowcollect.api.v1.recover.dto;

import java.util.List;

public class QueueActivityResponse {

    private final List<UpcomingItemResponse> upcoming;
    private final long totalUpcoming;
    private final List<ActivityItemResponse> activity;

    public QueueActivityResponse(List<UpcomingItemResponse> upcoming, long totalUpcoming,
                                 List<ActivityItemResponse> activity) {
        this.upcoming      = upcoming;
        this.totalUpcoming = totalUpcoming;
        this.activity      = activity;
    }

    public List<UpcomingItemResponse> getUpcoming() { return upcoming; }
    public long getTotalUpcoming()                  { return totalUpcoming; }
    public List<ActivityItemResponse> getActivity() { return activity; }
}
