package com.paidpeace.application.template;

import java.util.UUID;

import com.paidpeace.domain.template.Template;
import com.paidpeace.exception.code.TemplateErrorCode;
import com.paidpeace.exception.http.NotFoundException;
import com.paidpeace.exception.http.ValidationException;
import com.paidpeace.infrastructure.persistence.template.TemplateJpaRepository;

public class TemplateUtil {
    
    public static Template getTemplateOrThrow
    (
        UUID templateId,
        TemplateJpaRepository templateRepository
    ) {
        Template template = templateRepository.findById(templateId)
            .orElseThrow(() -> new NotFoundException(TemplateErrorCode.TEMPLATE_NOT_FOUND,
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
            throw new ValidationException(TemplateErrorCode.INVALID_TEMPLATE_FIELD,
                "Template and organization IDs are required");
        }
        
        Template template = getTemplateOrThrow(templateId, templateRepository);

        if(!template.getOrganization().getId().equals(organizationId)) {
            throw new ValidationException(TemplateErrorCode.INVALID_TEMPLATE_FIELD,
                "Template is not associated with the organization ID: " + organizationId);
        }

        return template;
    }
}
