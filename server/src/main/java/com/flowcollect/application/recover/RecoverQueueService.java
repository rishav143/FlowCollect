package com.flowcollect.application.recover;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.flowcollect.api.v1.recover.dto.ActivityItemResponse;
import com.flowcollect.api.v1.recover.dto.QueueActivityResponse;
import com.flowcollect.api.v1.recover.dto.QueueItemResponse;
import com.flowcollect.domain.invoice.followup.FollowUp;
import com.flowcollect.domain.invoice.followup.FollowUpStatus;
import com.flowcollect.infrastructure.persistence.invoice.FollowUpJpaRepository;

@Service
public class RecoverQueueService {

    private static final int QUEUE_DISPLAY_LIMIT   = 5;
    private static final int ACTIVITY_DISPLAY_LIMIT = 10;
    private static final int ACTIVITY_LOOKBACK_DAYS = 7;

    private final FollowUpJpaRepository followUpRepository;

    public RecoverQueueService(FollowUpJpaRepository followUpRepository) {
        this.followUpRepository = followUpRepository;
    }

    public QueueActivityResponse getQueueAndActivity(UUID organizationId) {
        // ── Send queue ────────────────────────────────────────────────────────
        List<FollowUp> allPending = followUpRepository.findPendingAutoQueue(organizationId);
        long totalPending = allPending.size();

        LocalDate today = LocalDate.now();
        List<QueueItemResponse> queue = allPending.stream()
                .limit(QUEUE_DISPLAY_LIMIT)
                .map(f -> new QueueItemResponse(
                        f.getId(),
                        f.getInvoice().getId(),
                        f.getInvoice().getInvoiceNumber(),
                        f.getInvoice().getCustomer() != null ? f.getInvoice().getCustomer().getName() : "Unknown",
                        f.getChannel(),
                        f.getInvoice().getDueDate() != null
                                ? (int) ChronoUnit.DAYS.between(f.getInvoice().getDueDate(), today)
                                : 0
                ))
                .toList();

        // ── Activity log ──────────────────────────────────────────────────────
        Instant since = Instant.now().minus(ACTIVITY_LOOKBACK_DAYS, ChronoUnit.DAYS);
        List<FollowUp> recent = followUpRepository
                .findRecentAutoActivity(organizationId, since, PageRequest.of(0, ACTIVITY_DISPLAY_LIMIT))
                .getContent();

        List<ActivityItemResponse> activity = recent.stream()
                .map(f -> {
                    Instant eventAt = f.getStatus() == FollowUpStatus.SENT
                            ? (f.getSentAt() != null ? f.getSentAt() : f.getUpdatedAt())
                            : f.getUpdatedAt();
                    String ruleName = f.getReminderRule() != null ? f.getReminderRule().getName() : null;
                    return new ActivityItemResponse(
                            f.getId(),
                            f.getInvoice().getInvoiceNumber(),
                            f.getInvoice().getCustomer() != null ? f.getInvoice().getCustomer().getName() : "Unknown",
                            f.getChannel(),
                            f.getStatus(),
                            ruleName,
                            eventAt
                    );
                })
                .toList();

        return new QueueActivityResponse(queue, totalPending, activity);
    }
}
