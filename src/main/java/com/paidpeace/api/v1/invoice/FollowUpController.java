package com.paidpeace.api.v1.invoice;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paidpeace.api.v1.invoice.dto.FollowUpRequest;
import com.paidpeace.api.v1.invoice.dto.FollowUpResponse;
import com.paidpeace.application.invoice.FollowUpService;
import com.paidpeace.domain.invoice.followup.FollowUp;
import com.paidpeace.domain.invoice.followup.FollowUpChannel;
import com.paidpeace.domain.invoice.followup.FollowUpStatus;
import com.paidpeace.domain.invoice.followup.FollowUpTriggerType;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices/{invoiceId}/followups")
public class FollowUpController {

    private final FollowUpService followUpService;

    public FollowUpController(FollowUpService followUpService) {
        this.followUpService = followUpService;
    }

    /**
     * Creates a follow-up.
     * Creates a new follow-up for an invoice.
     */
    @PostMapping
    public ResponseEntity<FollowUpResponse> createFollowUp(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody FollowUpRequest request
    ) {
        FollowUp created = followUpService.createFollowUp(invoiceId, request);
        return ResponseEntity.ok(FollowUpMapper.toResponse(created));
    }

    /**
     * Gets a follow-up by id.
     */
    @GetMapping("/{followUpId}")
    public ResponseEntity<FollowUpResponse> getFollowUp(
            @PathVariable UUID invoiceId,
            @PathVariable UUID followUpId
    ) {
        FollowUp followUp = followUpService.getFollowUp(invoiceId, followUpId);
        return ResponseEntity.ok(FollowUpMapper.toResponse(followUp));
    }

    /**
     * Gets follow-ups for an invoice by status, trigger type, and channel.
     */
    @GetMapping
    public ResponseEntity<Page<FollowUpResponse>> getFollowUps(
            @PathVariable UUID invoiceId,
            @RequestParam(required = false) FollowUpStatus status,
            @RequestParam(required = false) FollowUpTriggerType triggerType,
            @RequestParam(required = false) FollowUpChannel channel,
            Pageable pageable
    ) {
        Page<FollowUp> followUps = followUpService.getFollowUps(invoiceId, status, triggerType, channel, pageable);
        return ResponseEntity.ok(followUps.map(FollowUpMapper::toResponse));
    }

    /**
     * Updates a follow-up.
     * Updates the channel, trigger type, and template.
     */
    @PatchMapping("/{followUpId}")
    public ResponseEntity<FollowUpResponse> updateFollowUp(
            @PathVariable UUID invoiceId,
            @PathVariable UUID followUpId,
            @Valid @RequestBody FollowUpRequest request
    ) {
        FollowUp updated = followUpService.updateFollowUp(invoiceId, followUpId, request);
        return ResponseEntity.ok(FollowUpMapper.toResponse(updated));
    }

    /**
     * Sends a follow-up.
     * Sets the status to SENT and the sentAt to the current time.
     */
    @PatchMapping("/{followUpId}/send")
    public ResponseEntity<FollowUpResponse> sendFollowUp(
            @PathVariable UUID invoiceId,
            @PathVariable UUID followUpId
    ) {
        FollowUp sent = followUpService.sendFollowUp(invoiceId, followUpId);
        return ResponseEntity.ok(FollowUpMapper.toResponse(sent));
    }

    /**
     * Fails a follow-up.
     * Sets the status to FAILED.
     * Throws an exception if the follow-up is already sent.
     */
    @PatchMapping("/{followUpId}/fail")
    public ResponseEntity<FollowUpResponse> failFollowUp(
            @PathVariable UUID invoiceId,
            @PathVariable UUID followUpId
    ) {
        FollowUp failed = followUpService.failFollowUp(invoiceId, followUpId);
        return ResponseEntity.ok(FollowUpMapper.toResponse(failed));
    }
}
