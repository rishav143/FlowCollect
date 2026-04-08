package com.flowcollect.api.v1.invoice;

import jakarta.validation.Valid;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.security.RequireRole;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.flowcollect.api.v1.invoice.dto.FollowUpRequest;
import com.flowcollect.api.v1.invoice.dto.FollowUpResponse;
import com.flowcollect.api.v1.invoice.dto.MultiChannelFollowUpRequest;
import com.flowcollect.application.invoice.FollowUpService;
import com.flowcollect.domain.invoice.followup.FollowUp;
import com.flowcollect.domain.invoice.followup.FollowUpChannel;
import com.flowcollect.domain.invoice.followup.FollowUpStatus;
import com.flowcollect.domain.invoice.followup.FollowUpTriggerType;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/invoices/{invoiceId}/followups")
@RequireRole({ UserRole.ADMIN, UserRole.STAFF })
public class FollowUpController {

    private final FollowUpService followUpService;

    public FollowUpController(FollowUpService followUpService) {
        this.followUpService = followUpService;
    }

    // Creates a new follow-up for an invoice. 
    @PostMapping
    public ResponseEntity<FollowUpResponse> createFollowUp(
            @PathVariable UUID organizationId,
            @PathVariable UUID invoiceId,
            @Valid @RequestBody FollowUpRequest request
    ) {
        FollowUp created = followUpService.createFollowUp(organizationId, invoiceId, request);
        return ResponseEntity.status(201).body(FollowUpMapper.toResponse(created));
    }

    // Creates and dispatches manual follow-ups across multiple channels in a single call.
    @PostMapping("/dispatch")
    public ResponseEntity<List<FollowUpResponse>> createAndDispatchFollowUps(
            @PathVariable UUID organizationId,
            @PathVariable UUID invoiceId,
            @Valid @RequestBody MultiChannelFollowUpRequest request
    ) {
        List<FollowUp> followUps = followUpService.createAndDispatchFollowUps(organizationId, invoiceId, request);
        return ResponseEntity.status(201).body(followUps.stream().map(FollowUpMapper::toResponse).toList());
    }

    // Gets a follow-up by id.
    @GetMapping("/{followUpId}")
    public ResponseEntity<FollowUpResponse> getFollowUp(
            @PathVariable UUID organizationId,
            @PathVariable UUID invoiceId,
            @PathVariable UUID followUpId
    ) {
        FollowUp followUp = followUpService.getFollowUp(organizationId, invoiceId, followUpId);
        return ResponseEntity.ok(FollowUpMapper.toResponse(followUp));
    }

    // Gets follow-ups for an invoice filtered by status, triggerType, or channel.
    @GetMapping
    public ResponseEntity<Page<FollowUpResponse>> getFollowUps(
            @PathVariable UUID organizationId,
            @PathVariable UUID invoiceId,
            @RequestParam(required = false) FollowUpStatus status,
            @RequestParam(required = false) FollowUpTriggerType triggerType,
            @RequestParam(required = false) FollowUpChannel channel,
            @PageableDefault(size = 100, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<FollowUp> followUps = followUpService.getFollowUps(organizationId, invoiceId, status, triggerType, channel, pageable);
        return ResponseEntity.ok(followUps.map(FollowUpMapper::toResponse));
    }

    // Updates a follow-up's channel, template, scheduled date, or attachPdf flag.
    @PatchMapping("/{followUpId}")
    public ResponseEntity<FollowUpResponse> updateFollowUp(
            @PathVariable UUID organizationId,
            @PathVariable UUID invoiceId,
            @PathVariable UUID followUpId,
            @Valid @RequestBody FollowUpRequest request
    ) {
        FollowUp updated = followUpService.updateFollowUp(organizationId, invoiceId, followUpId, request);
        return ResponseEntity.ok(FollowUpMapper.toResponse(updated));
    }

    // Dispatches a PENDING follow-up via its channel. Marks it SENT or FAILED.
    @PatchMapping("/{followUpId}/send")
    public ResponseEntity<FollowUpResponse> dispatchFollowUp(
            @PathVariable UUID organizationId,
            @PathVariable UUID invoiceId,
            @PathVariable UUID followUpId
    ) {
        FollowUp dispatched = followUpService.dispatchFollowUp(organizationId, invoiceId, followUpId);
        return ResponseEntity.ok(FollowUpMapper.toResponse(dispatched));
    }

    // Manually marks a follow-up as FAILED.
    @PatchMapping("/{followUpId}/fail")
    public ResponseEntity<FollowUpResponse> failFollowUp(
            @PathVariable UUID organizationId,
            @PathVariable UUID invoiceId,
            @PathVariable UUID followUpId
    ) {
        FollowUp failed = followUpService.failFollowUp(organizationId, invoiceId, followUpId);
        return ResponseEntity.ok(FollowUpMapper.toResponse(failed));
    }

    // Cancels (skips) a PENDING follow-up. Used by the Recover queue panel.
    @PatchMapping("/{followUpId}/cancel")
    public ResponseEntity<FollowUpResponse> cancelFollowUp(
            @PathVariable UUID organizationId,
            @PathVariable UUID invoiceId,
            @PathVariable UUID followUpId
    ) {
        FollowUp followUp = followUpService.getFollowUp(organizationId, invoiceId, followUpId);
        FollowUp cancelled = followUpService.cancelFollowUp(followUp);
        return ResponseEntity.ok(FollowUpMapper.toResponse(cancelled));
    }

    // Deletes a follow-up (only PENDING or FAILED can be deleted).
    @DeleteMapping("/{followUpId}")
    public ResponseEntity<Void> deleteFollowUp(
            @PathVariable UUID organizationId,
            @PathVariable UUID invoiceId,
            @PathVariable UUID followUpId
    ) {
        followUpService.deleteFollowUp(organizationId, invoiceId, followUpId);
        return ResponseEntity.noContent().build();
    }
}
