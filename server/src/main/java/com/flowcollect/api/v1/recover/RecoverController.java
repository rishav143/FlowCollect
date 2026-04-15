package com.flowcollect.api.v1.recover;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flowcollect.api.v1.recover.dto.QueueActivityResponse;
import com.flowcollect.api.v1.recover.dto.RecoverStatsResponse;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.application.recover.RecoverQueueService;
import com.flowcollect.application.recover.RecoverStatsService;
import com.flowcollect.application.recover.RecoverStatsService.RecoverStats;
import com.flowcollect.domain.organization.OrgPlan;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.exception.http.PlanLimitException;
import com.flowcollect.security.RequireRole;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/recover")
@RequireRole({ UserRole.ADMIN, UserRole.STAFF })
public class RecoverController {

    private final RecoverStatsService  recoverStatsService;
    private final RecoverQueueService  recoverQueueService;
    private final OrganizationService  organizationService;

    public RecoverController(
            RecoverStatsService recoverStatsService,
            RecoverQueueService recoverQueueService,
            OrganizationService organizationService
    ) {
        this.recoverStatsService = recoverStatsService;
        this.recoverQueueService = recoverQueueService;
        this.organizationService = organizationService;
    }

    private void requirePro(UUID organizationId) {
        Organization org = organizationService.getAuthorizedById(organizationId);
        if (org.getPlan() != OrgPlan.PRO) {
            throw new PlanLimitException("Recover dashboard requires a PRO plan. Upgrade to continue.");
        }
    }

    /**
     * Returns Recover KPI stats for the organization.
     *
     * GET /api/v1/organizations/{organizationId}/recover/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<RecoverStatsResponse> getStats(@PathVariable UUID organizationId) {
        requirePro(organizationId);
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
        requirePro(organizationId);
        return ResponseEntity.ok(recoverQueueService.getQueueAndActivity(organizationId));
    }
}
