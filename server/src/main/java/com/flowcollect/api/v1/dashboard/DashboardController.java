package com.flowcollect.api.v1.dashboard;

import com.flowcollect.api.v1.dashboard.dto.DashboardStatsResponse;
import com.flowcollect.application.dashboard.DashboardService;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.security.RequireRole;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/dashboard")
@RequireRole({ UserRole.ADMIN, UserRole.STAFF })
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * GET /api/v1/organizations/{organizationId}/dashboard/stats
     *
     * Returns dashboard stats including needsAttention: OVERDUE invoices with no SENT
     * follow-up in the last 48 hours, sorted by dueDate ascending, limited to 4.
     */
    @GetMapping("/stats")
    public ResponseEntity<DashboardStatsResponse> getStats(@PathVariable UUID organizationId) {
        return ResponseEntity.ok(dashboardService.getStats(organizationId));
    }
}
