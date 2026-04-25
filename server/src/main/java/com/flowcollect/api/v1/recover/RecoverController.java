package com.flowcollect.api.v1.recover;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flowcollect.api.v1.recover.dto.QueueActivityResponse;
import com.flowcollect.api.v1.recover.dto.RecoverStatsResponse;
import com.flowcollect.application.recover.RecoverQueueService;
import com.flowcollect.application.recover.RecoverStatsService;
import com.flowcollect.application.recover.RecoverStatsService.RecoverStats;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.security.RequireRole;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/recover")
@RequireRole({ UserRole.ADMIN, UserRole.STAFF })
public class RecoverController {

    private final RecoverStatsService  recoverStatsService;
    private final RecoverQueueService  recoverQueueService;

    public RecoverController(
            RecoverStatsService recoverStatsService,
            RecoverQueueService recoverQueueService
    ) {
        this.recoverStatsService = recoverStatsService;
        this.recoverQueueService = recoverQueueService;
    }

    /**
     * Returns Recover KPI stats for the organization.
     *
     * GET /api/v1/organizations/{organizationId}/recover/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<RecoverStatsResponse> getStats(@PathVariable UUID organizationId) {
        RecoverStats stats = recoverStatsService.getStats(organizationId);
        return ResponseEntity.ok(new RecoverStatsResponse(
                stats.totalRecovered(),
                stats.activeRules(),
                stats.sentToday(),
                stats.pendingToday(),
                stats.sentThisWeek()
        ));
    }

    /**
     * Returns today's send queue and recent activity log for the Recover page.
     *
     * GET /api/v1/organizations/{organizationId}/recover/queue-activity
     */
    @GetMapping("/queue-activity")
    public ResponseEntity<QueueActivityResponse> getQueueActivity(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(recoverQueueService.getQueueAndActivity(organizationId));
    }
}
