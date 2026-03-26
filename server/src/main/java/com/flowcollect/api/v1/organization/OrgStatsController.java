package com.flowcollect.api.v1.organization;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flowcollect.api.v1.organization.dto.OrgStatsResponse;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.infrastructure.persistence.invoice.PaymentJpaRepository;
import com.flowcollect.security.RequireRole;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/stats")
@RequireRole({ UserRole.ADMIN, UserRole.STAFF })
public class OrgStatsController {

    private final OrganizationService    organizationService;
    private final PaymentJpaRepository   paymentRepository;

    public OrgStatsController(
            OrganizationService  organizationService,
            PaymentJpaRepository paymentRepository
    ) {
        this.organizationService = organizationService;
        this.paymentRepository   = paymentRepository;
    }

    /**
     * Returns lightweight KPI stats for the dashboard.
     * collectedThisMonth = sum of all payments recorded this calendar month,
     * including partial payments, using paidAt as the authoritative timestamp.
     */
    @GetMapping
    public ResponseEntity<OrgStatsResponse> getStats(@PathVariable UUID organizationId) {
        Organization org = organizationService.getById(organizationId);
        ZoneId tz        = org.getTimezone();

        LocalDate today      = LocalDate.now(tz);
        Instant   startOfMonth = today.withDayOfMonth(1).atStartOfDay(tz).toInstant();
        Instant   startOfNext  = today.plusMonths(1).withDayOfMonth(1).atStartOfDay(tz).toInstant();

        BigDecimal collected = paymentRepository.sumAmountByOrganizationAndPaidAtBetween(
                organizationId, startOfMonth, startOfNext);

        return ResponseEntity.ok(new OrgStatsResponse(collected));
    }
}
