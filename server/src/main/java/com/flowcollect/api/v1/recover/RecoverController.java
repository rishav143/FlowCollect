package com.flowcollect.api.v1.recover;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flowcollect.api.v1.recover.dto.RecoverStatsResponse;
import com.flowcollect.application.recover.RecoverStatsService;
import com.flowcollect.application.recover.RecoverStatsService.RecoverStats;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.security.RequireRole;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/recover")
@RequireRole({ UserRole.ADMIN, UserRole.STAFF })
public class RecoverController {

    private final RecoverStatsService recoverStatsService;

    public RecoverController(RecoverStatsService recoverStatsService) {
        this.recoverStatsService = recoverStatsService;
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
}
