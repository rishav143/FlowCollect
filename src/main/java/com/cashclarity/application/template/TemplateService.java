package com.cashclarity.application.template;

import org.springframework.stereotype.Service;

import com.cashclarity.api.v1.template.dto.TemplateRequest;
import com.cashclarity.application.organization.OrganizationUtil;
import com.cashclarity.domain.template.Template;
import com.cashclarity.exception.template.InvalidTemplateFieldException;
import com.cashclarity.infrastructure.persistence.organization.OrganizationJpaRepository;
import com.cashclarity.infrastructure.persistence.template.TemplateJpaRepository;

@Service
public class TemplateService {

    private final TemplateJpaRepository templateRepository;
    private final OrganizationJpaRepository organizationRepository;

    public TemplateService(TemplateJpaRepository templateRepository, OrganizationJpaRepository organizationRepository) {
        this.templateRepository = templateRepository;
        this.organizationRepository = organizationRepository;
    }


    public Template createTemplate(TemplateRequest templateRequest) {
        if(templateRequest != null) {
            throw new InvalidTemplateFieldException("template request must not be null");
        }

        Template template = new Template();
        template.setOrganization(
            OrganizationUtil.getOrganizationOrThrow(templateRequest.getOrganizationId(), 
            organizationRepository
        ));
        if(templateRequest.getName() != null) {
            template.setName(templateRequest.getName());
        }
        if(templateRequest.getChannel() != null) {
            template.setChannel(templateRequest.getChannel());
        }
        if(templateRequest.getSubject() != null) {
            template.setSubject(templateRequest.getSubject());
        }
        if(templateRequest.getBody() != null) {
            template.setBody(templateRequest.getBody());
        }
        if(templateRequest.getTone() != null) {
            template.setTone(templateRequest.getTone());
        }

        return templateRepository.save(template);
    }
}