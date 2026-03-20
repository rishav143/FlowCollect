package com.flowcollect.application.template;

import java.util.UUID;

import com.flowcollect.domain.template.Template;
import com.flowcollect.exception.http.NotFoundException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.template.TemplateJpaRepository;

public class TemplateUtil {
    
    public static Template getTemplateOrThrow
    (
        UUID templateId,
        TemplateJpaRepository templateRepository
    ) {
        Template template = templateRepository.findById(templateId)
            .orElseThrow(() -> new NotFoundException( 
                "Template not found with ID: " + templateId));
        return template;
    }

    public static Template validateTemplateWithOrganization
    (
        UUID templateId,
        UUID organizationId,
        TemplateJpaRepository templateRepository
    ) {
        if(templateId == null || organizationId == null) {
            throw new ValidationException( 
                "Template and organization IDs are required");
        }
        
        Template template = getTemplateOrThrow(templateId, templateRepository);

        if(!template.getOrganization().getId().equals(organizationId)) {
            throw new NotFoundException("Template not found with ID: " + templateId);
        }

        return template;
    }
}
