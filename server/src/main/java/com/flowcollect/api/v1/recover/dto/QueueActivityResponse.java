package com.flowcollect.api.v1.recover.dto;

import java.util.List;

public class QueueActivityResponse {

    private final List<QueueItemResponse> queue;
    private final long totalPending;       // total in queue, queue list may be limited
    private final List<ActivityItemResponse> activity;

    public QueueActivityResponse(List<QueueItemResponse> queue, long totalPending,
                                 List<ActivityItemResponse> activity) {
        this.queue        = queue;
        this.totalPending = totalPending;
        this.activity     = activity;
    }

    public List<QueueItemResponse> getQueue()          { return queue; }
    public long getTotalPending()                      { return totalPending; }
    public List<ActivityItemResponse> getActivity()    { return activity; }
}
