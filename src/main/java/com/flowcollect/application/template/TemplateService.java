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
import com.flowcollect.domain.template.Template;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.template.TemplateJpaRepository;

import jakarta.persistence.criteria.Predicate;
import jakarta.transaction.Transactional;

@Service
public class TemplateService {

    private final TemplateJpaRepository templateRepository;
    private final OrganizationService organizationService;
    
    public TemplateService
    (
        TemplateJpaRepository templateRepository, 
        OrganizationService organizationService
    ) {
        this.templateRepository = templateRepository;
        this.organizationService = organizationService;
    }


    /**
     * Create a new template for an organization.
     */
    @Transactional
    public Template createTemplate
    (
        UUID organizationId,
        TemplateRequest templateRequest
    ) {
        if(templateRequest == null) {
            throw new ValidationException( 
                "Template request must not be null");
        }
        Organization organization = organizationService.getById(organizationId);

        Template template = new Template();
        template.setOrganization(organization);
        if(templateRequest.getName() == null || templateRequest.getName().isBlank()) {
            throw new ValidationException( 
                "Name must not be null or blank");
        }
        if(templateRepository.existsByNameAndOrganizationId(templateRequest.getName(), organizationId)) {
            throw new ValidationException( 
                "A template with the name '" + templateRequest.getName() + "' already exists for this organization");
        }
        template.setName(templateRequest.getName());
        if (templateRepository.existsByNameAndOrganizationId(templateRequest.getName(), organizationId)) {
            throw new ValidationException("A template with the name '" + templateRequest.getName() + "' already exists for this organization");
        }
        if(templateRequest.getChannel() == null) {
            throw new ValidationException("Channel must not be null");
        }
        template.setChannel(templateRequest.getChannel());
        if(templateRequest.getSubject() != null) {
            template.setSubject(templateRequest.getSubject());
        }
        if(templateRequest.getTone() == null) {
            throw new ValidationException("Body must not be null");
        }
        template.setBody(templateRequest.getBody());
        if(templateRequest.getTone() == null) {
            throw new ValidationException("Tone must not be null");
        }
        template.setTone(templateRequest.getTone());

        return templateRepository.save(template);
    }


    // Get a template by id with organization context
    public Template getTemplateById
    (
        UUID organizationId,
        UUID templateId
    ) {
        organizationService.getById(organizationId);
        return TemplateUtil.validateTemplateWithOrganization(templateId, organizationId, templateRepository);
    }

    // Get a template by id without organization context
    public Template getTemplateById(UUID templateId) {
        Template template = TemplateUtil.getTemplateOrThrow(templateId, templateRepository);
        organizationService.getById(template.getOrganization().getId());
        return template;
    }

    // Get all templates for an organization with pagination.
    public Page<Template> getAllTemplates
    (
        UUID organizationId,
        String name, 
        String channel, 
        String tone, 
        Pageable pageable
    ) {
        organizationService.getById(organizationId);
        PaginationUtils.validatePageable(pageable);

        Specification<Template> spec = (root, query, cb) -> {
            Predicate p = cb.equal(root.get("organization").get("id"), organizationId);
            if(name != null && !name.isBlank()) {
                p = cb.and(p, cb.like(root.get("name"), "%" + name + "%"));
            }
            if(channel != null && !channel.isBlank()) {
                p = cb.and(p, cb.like(root.get("channel"), "%" + channel + "%"));
            }
            if(tone != null && !tone.isBlank()) {
                p = cb.and(p, cb.like(root.get("tone"), "%" + tone + "%"));
            }
            return p;
        };

        return templateRepository.findAll(spec, pageable);
    }


    /**
     * Update a template.
     */
    public Template updateTemplate
    (
        UUID organizationId, 
        UUID templateId, 
        TemplateRequest templateRequest
    ) {
        if (templateRequest == null) {
            throw new ValidationException("Template request must not be null");
        }
        organizationService.getById(organizationId);
        TemplateUtil.validateTemplateWithOrganization(templateId, organizationId, templateRepository);

        Template template = TemplateUtil.getTemplateOrThrow(templateId, templateRepository);
        if(templateRequest.getName() != null) {
            template.setName(templateRequest.getName());
        } 
        if(templateRequest.getChannel() != null) {
            template.setChannel(templateRequest.getChannel());
        }
        if(templateRequest.getTone() != null) {
            template.setTone(templateRequest.getTone());
        }
        if(templateRequest.getSubject() != null) {
            template.setSubject(templateRequest.getSubject());
        }
        if(templateRequest.getBody() != null) {
            template.setBody(templateRequest.getBody());
        }

        return templateRepository.save(template);
    }


    /**
     * Delete a template by id.
     */
    public void deleteTemplateById
    (
        UUID organizationId,
        UUID templateId
    ) {
        organizationService.getById(organizationId);
        TemplateUtil.validateTemplateWithOrganization(templateId, organizationId, templateRepository);

        templateRepository.deleteById(templateId);
    }

    public Template activateTemplate
    (
        UUID organizationId, 
        UUID id
    ) {
        organizationService.getById(organizationId);
        TemplateUtil.validateTemplateWithOrganization(id, organizationId, templateRepository);

        Template template = TemplateUtil.getTemplateOrThrow(id, templateRepository);
        template.activate();
        return templateRepository.save(template);
    }

    public Template deactivateTemplate
    (
        UUID organizationId,
        UUID id
    ) {
        organizationService.getById(organizationId);
        TemplateUtil.validateTemplateWithOrganization(id, organizationId, templateRepository);

        Template template = TemplateUtil.getTemplateOrThrow(id, templateRepository);
        template.deactivate();
        return templateRepository.save(template);
    }
}