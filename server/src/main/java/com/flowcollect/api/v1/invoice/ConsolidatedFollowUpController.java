package com.flowcollect.api.v1.invoice;

import com.flowcollect.api.v1.invoice.dto.ConsolidatedFollowUpRequest;
import com.flowcollect.api.v1.invoice.dto.ConsolidatedFollowUpResponse;
import com.flowcollect.application.invoice.FollowUpService;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.security.RequireRole;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Handles consolidated follow-up dispatch — one message per channel sent to a customer
 * covering all their outstanding invoices, with one PDF per invoice attached.
 *
 * Lives at the org level (not under /invoices/{id}) because it spans multiple invoices.
 */
@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/followups")
@RequireRole({ UserRole.ADMIN, UserRole.STAFF })
public class ConsolidatedFollowUpController {

    private final FollowUpService followUpService;

    public ConsolidatedFollowUpController(FollowUpService followUpService) {
        this.followUpService = followUpService;
    }

    /**
     * Sends one consolidated message per channel to a customer covering multiple invoices.
     *
     * POST /api/v1/organizations/{organizationId}/followups/consolidated
     *
     * Body: {
     *   "invoiceIds": ["uuid1", "uuid2"],
     *   "channels": ["EMAIL"],
     *   "templateId": "uuid",
     *   "attachPdf": true
     * }
     *
     * Returns one result per channel with status and follow-up IDs for audit.
     */
    @PostMapping("/consolidated")
    public ResponseEntity<List<ConsolidatedFollowUpResponse>> consolidatedDispatch(
            @PathVariable UUID organizationId,
            @Valid @RequestBody ConsolidatedFollowUpRequest request
    ) {
        List<ConsolidatedFollowUpResponse> results =
                followUpService.consolidatedDispatch(organizationId, request);
        return ResponseEntity.status(201).body(results);
    }
}
