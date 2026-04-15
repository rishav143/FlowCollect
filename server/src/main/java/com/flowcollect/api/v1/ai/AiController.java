package com.flowcollect.api.v1.ai;

import com.flowcollect.api.v1.ai.dto.AiInsightResponse;
import com.flowcollect.api.v1.ai.dto.AiTemplateResponse;
import com.flowcollect.api.v1.ai.dto.AskInsightRequest;
import com.flowcollect.api.v1.ai.dto.EnhanceTemplateRequest;
import com.flowcollect.api.v1.ai.dto.GenerateTemplateRequest;
import com.flowcollect.application.ai.AiInsightService;
import com.flowcollect.application.ai.AiTemplateService;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.domain.organization.OrgPlan;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.exception.http.PlanLimitException;
import com.flowcollect.security.RequireRole;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/ai")
@RequireRole({ UserRole.ADMIN, UserRole.STAFF })
public class AiController {

    private final AiTemplateService  aiTemplateService;
    private final AiInsightService   aiInsightService;
    private final OrganizationService organizationService;

    public AiController(
            AiTemplateService   aiTemplateService,
            AiInsightService    aiInsightService,
            OrganizationService organizationService
    ) {
        this.aiTemplateService   = aiTemplateService;
        this.aiInsightService    = aiInsightService;
        this.organizationService = organizationService;
    }

    private void requirePro(UUID organizationId) {
        Organization org = organizationService.getAuthorizedById(organizationId);
        if (org.getPlan() != OrgPlan.PRO) {
            throw new PlanLimitException("AI features require a PRO plan. Upgrade to continue.");
        }
    }

    /**
     * Generates a new payment reminder template from scratch.
     *
     * POST /api/v1/organizations/{organizationId}/ai/templates/generate
     * Body: { "channel": "EMAIL|SMS|WHATSAPP", "tone": "POLITE|NEUTRAL|FIRM" }
     *
     * Returns AI-generated subject (null for SMS/WHATSAPP) and body.
     * The caller can review it and then save it via the standard template endpoint.
     */
    @PostMapping("/templates/generate")
    public ResponseEntity<AiTemplateResponse> generateTemplate(
            @PathVariable UUID organizationId,
            @RequestBody @Valid GenerateTemplateRequest request
    ) {
        requirePro(organizationId);
        AiTemplateService.AiTemplateResult result = aiTemplateService.generateTemplate(
                organizationId,
                request.getChannel(),
                request.getTone()
        );
        return ResponseEntity.ok(new AiTemplateResponse(result.subject(), result.body()));
    }

    /**
     * Rewrites an existing template body/subject to better match a target tone.
     * All {{placeholder}} variables are preserved exactly.
     *
     * POST /api/v1/organizations/{organizationId}/ai/templates/enhance
     * Body: { "channel": "EMAIL", "tone": "FIRM", "subject": "...", "body": "..." }
     *
     * Returns improved subject (null for SMS/WHATSAPP) and body for review before saving.
     */
    @PostMapping("/templates/enhance")
    public ResponseEntity<AiTemplateResponse> enhanceTemplate(
            @PathVariable UUID organizationId,
            @RequestBody @Valid EnhanceTemplateRequest request
    ) {
        requirePro(organizationId);
        AiTemplateService.AiTemplateResult result = aiTemplateService.enhanceTemplate(
                organizationId,
                request.getChannel(),
                request.getTone(),
                request.getSubject(),
                request.getBody()
        );
        return ResponseEntity.ok(new AiTemplateResponse(result.subject(), result.body()));
    }

    /**
     * Flexible insight endpoint — ask any payment question with optional data filters.
     *
     * POST /api/v1/organizations/{organizationId}/ai/insights/ask
     * Body: {
     *   "question": "Which customers should I follow up with this week?",
     *   "customerId": "<uuid>",          // optional — scope to one customer
     *   "lifeCycleStatus": "ISSUED",     // optional — DRAFT|ISSUED|PARTIALLY_PAID|PAID|CANCELLED
     *   "timeStatus": "OVERDUE",         // optional — NOT_DUE|DUE_TODAY|OVERDUE
     *   "channel": "EMAIL",              // optional — EMAIL|SMS|WHATSAPP (scopes follow-up data)
     *   "fromDate": "2026-01-01",        // optional — invoice issue date lower bound
     *   "toDate": "2026-03-31"           // optional — invoice issue date upper bound
     * }
     */
    @PostMapping("/insights/ask")
    public ResponseEntity<AiInsightResponse> askInsight(
            @PathVariable UUID organizationId,
            @RequestBody @Valid AskInsightRequest request
    ) {
        requirePro(organizationId);
        String insights = aiInsightService.ask(organizationId, request);
        return ResponseEntity.ok(new AiInsightResponse(insights, false, java.time.Instant.now()));
    }

    /**
     * Returns AI payment health overview for the organization.
     * Served from cache if generated today (org timezone); regenerates otherwise.
     *
     * GET /api/v1/organizations/{organizationId}/ai/insights/overview
     * GET /api/v1/organizations/{organizationId}/ai/insights/overview?forceRefresh=true
     */
    @GetMapping("/insights/overview")
    public ResponseEntity<AiInsightResponse> overviewInsights(
            @PathVariable UUID organizationId,
            @RequestParam(defaultValue = "false") boolean forceRefresh
    ) {
        requirePro(organizationId);
        AiInsightService.OverviewInsightResult result = aiInsightService.getOverviewInsights(organizationId, forceRefresh);
        return ResponseEntity.ok(new AiInsightResponse(result.insights(), result.cached(), result.generatedAt()));
    }

    /**
     * Generates an AI payment behavior profile for a specific customer.
     * Covers reliability assessment, outstanding balance, and recommended next actions.
     *
     * GET /api/v1/organizations/{organizationId}/ai/insights/customers/{customerId}
     */
    @GetMapping("/insights/customers/{customerId}")
    public ResponseEntity<AiInsightResponse> customerInsights(
            @PathVariable UUID organizationId,
            @PathVariable UUID customerId
    ) {
        requirePro(organizationId);
        String insights = aiInsightService.generateCustomerInsights(organizationId, customerId);
        return ResponseEntity.ok(new AiInsightResponse(insights, false, java.time.Instant.now()));
    }
}
