package com.flowcollect.api.v1.template;

import java.util.UUID;

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

import com.flowcollect.api.v1.template.dto.TemplateMapper;
import com.flowcollect.api.v1.template.dto.TemplateRequest;
import com.flowcollect.api.v1.template.dto.TemplateResponse;
import com.flowcollect.application.template.TemplateService;
import com.flowcollect.domain.template.Template;
import com.flowcollect.domain.template.TemplateChannel;
import com.flowcollect.domain.template.TemplateTone;
import com.flowcollect.domain.user.UserRole;
import com.flowcollect.security.RequireRole;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/templates")
@RequireRole({ UserRole.ADMIN, UserRole.STAFF })
public class TemplateController {
    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * Create a new template.
     */
    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate
    (
        @PathVariable UUID organizationId, 
        @RequestBody @Valid TemplateRequest templateRequest
    ) {
        Template template = templateService.createTemplate(organizationId, templateRequest);
        return ResponseEntity.status(201).body(TemplateMapper.toResponse(template));
    }

    /**
     * Get a template by id.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> getTemplateById
    (
        @PathVariable UUID organizationId, 
        @PathVariable UUID id
    ) {
        Template template = templateService.getTemplateById(organizationId, id);
        return ResponseEntity.ok(TemplateMapper.toResponse(template));
    }

    /**
     * Get all templates with pagination.
     */
    @GetMapping
    public ResponseEntity<Page<TemplateResponse>> getAllTemplates(
        @PathVariable UUID organizationId,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) TemplateChannel channel,
        @RequestParam(required = false) TemplateTone tone,
        Pageable pageable
    ) {
        Page<Template> templates = templateService.getAllTemplates(organizationId, name, channel, tone, pageable);
        return ResponseEntity.ok(templates.map(TemplateMapper::toResponse));
    }

    /**
     * Update a template.
     */
    @PatchMapping("/{id}")
    public ResponseEntity<TemplateResponse> updateTemplate
    (
        @PathVariable UUID organizationId,
        @PathVariable UUID id, 
        @RequestBody @Valid TemplateRequest templateRequest
    ) {
        Template template = templateService.updateTemplate(organizationId, id, templateRequest);
        return ResponseEntity.ok(TemplateMapper.toResponse(template));
    }

    /**
     * Delete a template.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplateById
    (
        @PathVariable UUID organizationId, 
        @PathVariable UUID id
    ) {
        templateService.deleteTemplateById(organizationId, id);
        return ResponseEntity.noContent().build();
    }

    // Activate a template.
    @PatchMapping("/{id}/activate")
    public ResponseEntity<TemplateResponse> activateTemplate
    (
        @PathVariable UUID organizationId, 
        @PathVariable UUID id
    ) {
        Template template = templateService.activateTemplate(organizationId, id);
        return ResponseEntity.ok(TemplateMapper.toResponse(template));
    }

    // Deactivate a template.
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<TemplateResponse> deactivateTemplate
    (
        @PathVariable UUID organizationId, 
        @PathVariable UUID id
    ) {
        Template template = templateService.deactivateTemplate(organizationId, id);
        return ResponseEntity.ok(TemplateMapper.toResponse(template));
    }
}
