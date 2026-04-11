package com.flowcollect.api.v1.reminderrule;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

import com.flowcollect.api.v1.reminderrule.dto.ReminderRuleMapper;
import com.flowcollect.api.v1.reminderrule.dto.ReminderRuleRequest;
import com.flowcollect.api.v1.reminderrule.dto.ReminderRuleResponse;
import com.flowcollect.application.reminder.ReminderRuleService;
import com.flowcollect.domain.reminder.ReminderRule;
import com.flowcollect.domain.template.Template;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.infrastructure.persistence.template.TemplateJpaRepository;
import com.flowcollect.security.RequireRole;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/reminder-rules")
@RequireRole({ UserRole.ADMIN, UserRole.STAFF })
public class ReminderRuleController {

    private final ReminderRuleService   reminderRuleService;
    private final TemplateJpaRepository templateRepository;

    public ReminderRuleController(
            ReminderRuleService reminderRuleService,
            TemplateJpaRepository templateRepository) {
        this.reminderRuleService = reminderRuleService;
        this.templateRepository  = templateRepository;
    }

    // Create a new reminder rule
    @PostMapping
    public ResponseEntity<ReminderRuleResponse> createReminderRule(
        @PathVariable UUID organizationId,
        @RequestBody ReminderRuleRequest request
    ) {
        ReminderRule rule = reminderRuleService.createReminderRule(organizationId, request);
        return ResponseEntity.status(201).body(ReminderRuleMapper.toResponse(rule, resolveNames(rule)));
    }

    // Get a reminder rule by ID
    @GetMapping("/{reminderRuleId}")
    public ResponseEntity<ReminderRuleResponse> getReminderRule(
        @PathVariable UUID organizationId,
        @PathVariable UUID reminderRuleId
    ) {
        ReminderRule rule = reminderRuleService.getReminderRule(organizationId, reminderRuleId);
        return ResponseEntity.ok(ReminderRuleMapper.toResponse(rule, resolveNames(rule)));
    }

    // Get all reminder rules with optional name filter
    @GetMapping
    public ResponseEntity<Page<ReminderRuleResponse>> getAllReminderRules(
        @PathVariable UUID organizationId,
        @RequestParam(required = false) String name,
        Pageable pageable
    ) {
        Page<ReminderRule> rules = reminderRuleService.getAllReminderRules(organizationId, name, pageable);
        // Batch-resolve all occurrence template IDs across the whole page in one query
        List<UUID> allIds = rules.stream()
            .flatMap(r -> r.getOccurrenceTemplateIds().stream())
            .distinct()
            .collect(Collectors.toList());
        Map<UUID, String> nameById = buildNameMap(allIds);
        return ResponseEntity.ok(rules.map(r -> ReminderRuleMapper.toResponse(r, nameById)));
    }

    // Update a reminder rule (partial)
    @PatchMapping("/{reminderRuleId}")
    public ResponseEntity<ReminderRuleResponse> updateReminderRule(
        @PathVariable UUID organizationId,
        @PathVariable UUID reminderRuleId,
        @RequestBody ReminderRuleRequest request
    ) {
        ReminderRule rule = reminderRuleService.updateReminderRule(organizationId, reminderRuleId, request);
        return ResponseEntity.ok(ReminderRuleMapper.toResponse(rule, resolveNames(rule)));
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
    @PatchMapping("/{reminderRuleId}/activate")
    public ResponseEntity<ReminderRuleResponse> activateReminderRule(
        @PathVariable UUID organizationId,
        @PathVariable UUID reminderRuleId
    ) {
        ReminderRule rule = reminderRuleService.activateReminderRule(organizationId, reminderRuleId);
        return ResponseEntity.ok(ReminderRuleMapper.toResponse(rule, resolveNames(rule)));
    }

    // Deactivate a reminder rule
    @PatchMapping("/{reminderRuleId}/deactivate")
    public ResponseEntity<ReminderRuleResponse> deactivateReminderRule(
        @PathVariable UUID organizationId,
        @PathVariable UUID reminderRuleId
    ) {
        ReminderRule rule = reminderRuleService.deactivateReminderRule(organizationId, reminderRuleId);
        return ResponseEntity.ok(ReminderRuleMapper.toResponse(rule, resolveNames(rule)));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Map<UUID, String> resolveNames(ReminderRule rule) {
        return buildNameMap(rule.getOccurrenceTemplateIds());
    }

    private Map<UUID, String> buildNameMap(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Collections.emptyMap();
        return templateRepository.findAllById(ids).stream()
            .collect(Collectors.toMap(Template::getId, Template::getName));
    }
}
