package com.cashclarity.exception.template;

import java.util.UUID;

import com.cashclarity.api.error.ErrorCode;
import com.cashclarity.exception.base.NotFoundException;

public class TemplateNotFoundException extends NotFoundException {
    public TemplateNotFoundException(String reason) {
        super(ErrorCode.TEMPLATE_NOT_FOUND, 
            reason);
    }
    public TemplateNotFoundException(UUID templateId) {
        super(ErrorCode.TEMPLATE_NOT_FOUND, 
            "Template not found for id '" + templateId.toString() + "'.");
    }
}

