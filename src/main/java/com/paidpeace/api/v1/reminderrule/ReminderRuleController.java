package com.paidpeace.api.v1.reminderrule;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.paidpeace.api.v1.reminderrule.dto.ReminderRuleMapper;
import com.paidpeace.api.v1.reminderrule.dto.ReminderRuleRequest;
import com.paidpeace.api.v1.reminderrule.dto.ReminderRuleResponse;
import com.paidpeace.application.reminder.ReminderRuleService;
import com.paidpeace.domain.reminder.ReminderRule;
import com.paidpeace.domain.user.UserRole;
import com.paidpeace.security.RequireRole;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/reminder-rules")
@RequireRole({ UserRole.ADMIN, UserRole.STAFF })
public class ReminderRuleController {

    private final ReminderRuleService reminderRuleService;

    public ReminderRuleController(ReminderRuleService reminderRuleService) {
        this.reminderRuleService = reminderRuleService;
    }

    // Create a new reminder rule
    @PostMapping
    public ResponseEntity<ReminderRuleResponse> createReminderRule
    (
        @PathVariable UUID organizationId,
        @RequestBody ReminderRuleRequest reminderRuleRequest
    ) {
        ReminderRule reminderRule = reminderRuleService.createReminderRule(organizationId, reminderRuleRequest);
        return ResponseEntity.ok(ReminderRuleMapper.toResponse(reminderRule));
    }

    // Get a reminder rule by ID
    @GetMapping("/{reminderRuleId}")
    public ResponseEntity<ReminderRuleResponse> getReminderRule(
        @PathVariable UUID organizationId,
        @PathVariable UUID reminderRuleId
    ) {
        ReminderRule reminderRule = reminderRuleService.getReminderRule(organizationId, reminderRuleId);
        return ResponseEntity.ok(ReminderRuleMapper.toResponse(reminderRule));
    }

    // Get all reminder rules
    @GetMapping
    public ResponseEntity<Page<ReminderRuleResponse>> getAllReminderRules
    (
        @PathVariable UUID organizationId,
        @RequestParam(required = false) String name,
        Pageable pageable
    ) {
        Page<ReminderRule> reminderRules = reminderRuleService.getAllReminderRules(organizationId, name, pageable);
        return ResponseEntity.ok(reminderRules.map(ReminderRuleMapper::toResponse));
    }

    // Update a reminder rule
    @PatchMapping("/{reminderRuleId}")
    public ResponseEntity<ReminderRuleResponse> updateReminderRule
    (
        @PathVariable UUID organizationId,
        @PathVariable UUID reminderRuleId,
        @RequestBody ReminderRuleRequest reminderRuleRequest
    ) {
        ReminderRule reminderRule = reminderRuleService.updateReminderRule(organizationId, reminderRuleId, reminderRuleRequest);
        return ResponseEntity.ok(ReminderRuleMapper.toResponse(reminderRule));
    }

    // Delete a reminder rule
    @DeleteMapping("/{reminderRuleId}")
    public ResponseEntity<Void> deleteReminderRule(
        @PathVariable UUID organizationId,
        @PathVariable UUID reminderRuleId
    ) {
        reminderRuleService.deleteReminderRule(organizationId, reminderRuleId);
        return ResponseEntity.noContent().build();
    }

    // Activate a reminder rule
    @PostMapping("/{reminderRuleId}/activate")
    public ResponseEntity<Void> activateReminderRule(
        @PathVariable UUID organizationId,
        @PathVariable UUID reminderRuleId
    ) {
        reminderRuleService.activateReminderRule(organizationId, reminderRuleId);
        return ResponseEntity.noContent().build();
    }

    // Deactivate a reminder rule
    @PostMapping("/{reminderRuleId}/deactivate")
    public ResponseEntity<Void> deactivateReminderRule(
        @PathVariable UUID organizationId,
        @PathVariable UUID reminderRuleId
    ) {
        reminderRuleService.deactivateReminderRule(organizationId, reminderRuleId);
        return ResponseEntity.noContent().build();
    }
}
