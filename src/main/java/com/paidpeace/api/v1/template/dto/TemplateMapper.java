package com.paidpeace.api.v1.template.dto;

import com.paidpeace.domain.template.Template;

public class TemplateMapper {
    public static TemplateResponse toResponse(Template template) {
        TemplateResponse response = new TemplateResponse();
        response.setId(template.getId());
        response.setName(template.getName());
        response.setChannel(template.getChannel());
        response.setSubject(template.getSubject());
        response.setBody(template.getBody());
        response.setTone(template.getTone());
        response.setActive(template.isActive());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());
        return response;
    }
}
