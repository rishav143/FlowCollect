package com.flowcollect.application.template;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.flowcollect.api.v1.template.dto.TemplateRequest;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.common.PaginationUtils;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.reminder.RuleMode;
import com.flowcollect.domain.template.Template;
import com.flowcollect.domain.template.TemplateChannel;
import com.flowcollect.domain.template.TemplateTone;
import com.flowcollect.exception.http.ConflictException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.invoice.FollowUpJpaRepository;
import com.flowcollect.infrastructure.persistence.reminder.ReminderRuleJpaRepository;
import com.flowcollect.infrastructure.persistence.template.TemplateJpaRepository;

import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;

@Service
public class TemplateService {

    private final TemplateJpaRepository templateRepository;
    private final OrganizationService organizationService;
    private final ReminderRuleJpaRepository reminderRuleRepository;
    private final FollowUpJpaRepository followUpRepository;

    public TemplateService(
        TemplateJpaRepository templateRepository,
        OrganizationService organizationService,
        ReminderRuleJpaRepository reminderRuleRepository,
        FollowUpJpaRepository followUpRepository
    ) {
        this.templateRepository = templateRepository;
        this.organizationService = organizationService;
        this.reminderRuleRepository = reminderRuleRepository;
        this.followUpRepository = followUpRepository;
    }

    // Create a new template for an organization.
    @Transactional
    public Template createTemplate(UUID organizationId, TemplateRequest request) {
        if (request == null) {
            throw new ValidationException("Template request must not be null");
        }
        Organization organization = organizationService.getById(organizationId);

        if (request.getName() == null || request.getName().isBlank()) {
            throw new ValidationException("Name must not be null or blank");
        }
        if (templateRepository.existsByNameAndOrganizationId(request.getName().trim(), organizationId)) {
            throw new ValidationException("A template named '" + request.getName().trim() + "' already exists for this organization");
        }
        if (request.getChannel() == null) {
            throw new ValidationException("Channel must not be null");
        }
        if (request.getBody() == null || request.getBody().isBlank()) {
            throw new ValidationException("Body must not be null or blank");
        }
        if (request.getTone() == null) {
            throw new ValidationException("Tone must not be null");
        }

        Template template = new Template();
        template.setOrganization(organization);
        template.setName(request.getName().trim());
        template.setChannel(request.getChannel());
        template.setBody(request.getBody());
        template.setTone(request.getTone());
        template.setMode(request.getMode() != null ? request.getMode() : RuleMode.MANUAL);
        if (request.getSubject() != null) {
            template.setSubject(request.getSubject());
        }

        return templateRepository.save(template);
    }

    // Get a template by id with organization context.
    public Template getTemplateById(UUID organizationId, UUID templateId) {
        organizationService.getById(organizationId);
        return TemplateUtil.validateTemplateWithOrganization(templateId, organizationId, templateRepository);
    }

    // Get a template by id without organization context (internal use).
    // System-defined templates have no org — skip org validation for those.
    public Template getTemplateById(UUID templateId) {
        Template template = TemplateUtil.getTemplateOrThrow(templateId, templateRepository);
        if (template.getOrganization() != null) {
            organizationService.getById(template.getOrganization().getId());
        }
        return template;
    }

    // Get all templates for an organization with pagination.
    public Page<Template> getAllTemplates(
        UUID organizationId,
        String name,
        TemplateChannel channel,
        TemplateTone tone,
        RuleMode mode,
        Pageable pageable
    ) {
        organizationService.getById(organizationId);
        PaginationUtils.validatePageable(pageable);

        Specification<Template> spec = (root, query, cb) -> {
            // Include org-owned templates OR system-defined templates (visible to all orgs)
            Predicate orgOwned = cb.equal(root.get("organization").get("id"), organizationId);
            Predicate systemDefined = cb.isTrue(root.get("systemDefined"));
            Predicate p = cb.or(orgOwned, systemDefined);
            if (name != null && !name.isBlank()) {
                p = cb.and(p, cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            if (channel != null) {
                p = cb.and(p, cb.equal(root.get("channel"), channel));
            }
            if (tone != null) {
                p = cb.and(p, cb.equal(root.get("tone"), tone));
            }
            if (mode != null) {
                p = cb.and(p, cb.equal(root.get("mode"), mode));
            }
            return p;
        };

        return templateRepository.findAll(spec, pageable);
    }

    // Update a template.
    @Transactional
    public Template updateTemplate(UUID organizationId, UUID templateId, TemplateRequest request) {
        if (request == null) {
            throw new ValidationException("Template request must not be null");
        }
        organizationService.getById(organizationId);
        Template template = TemplateUtil.validateTemplateWithOrganization(templateId, organizationId, templateRepository);

        if (request.getName() != null) {
            if (request.getName().isBlank()) {
                throw new ValidationException("Name must not be blank");
            }
            String trimmed = request.getName().trim();
            if (!trimmed.equals(template.getName()) &&
                    templateRepository.existsByNameAndOrganizationId(trimmed, organizationId)) {
                throw new ValidationException("A template named '" + trimmed + "' already exists for this organization");
            }
            template.setName(trimmed);
        }
        if (request.getChannel() != null) {
            template.setChannel(request.getChannel());
        }
        if (request.getTone() != null) {
            template.setTone(request.getTone());
        }
        if (request.getSubject() != null) {
            template.setSubject(request.getSubject());
        }
        if (request.getBody() != null) {
            if (request.getBody().isBlank()) {
                throw new ValidationException("Body must not be blank");
            }
            template.setBody(request.getBody());
        }
        if (request.getMode() != null) {
            template.setMode(request.getMode());
        }

        return templateRepository.save(template);
    }

    // Delete a template by id.
    @Transactional
    public void deleteTemplateById(UUID organizationId, UUID templateId) {
        organizationService.getById(organizationId);
        Template template = TemplateUtil.validateTemplateWithOrganization(templateId, organizationId, templateRepository);
        if (reminderRuleRepository.existsByTemplateId(template.getId())) {
            throw new ConflictException("Cannot delete this template because it is used by one or more reminder rules. Remove or reassign those rules first.");
        }
        // Detach historical follow-ups before deleting (column is nullable, safe to null out)
        followUpRepository.detachTemplate(template.getId());
        templateRepository.deleteById(template.getId());
    }

    // Activate a template.
    @Transactional
    public Template activateTemplate(UUID organizationId, UUID templateId) {
        organizationService.getById(organizationId);
        Template template = TemplateUtil.validateTemplateWithOrganization(templateId, organizationId, templateRepository);
        template.activate();
        return templateRepository.save(template);
    }

    // Deactivate a template.
    @Transactional
    public Template deactivateTemplate(UUID organizationId, UUID templateId) {
        organizationService.getById(organizationId);
        Template template = TemplateUtil.validateTemplateWithOrganization(templateId, organizationId, templateRepository);
        template.deactivate();
        return templateRepository.save(template);
    }
}
