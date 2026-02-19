package com.cashclarity.exception.template;

import java.util.UUID;

import com.cashclarity.api.error.ErrorCode;
import com.cashclarity.exception.base.ValidationException;
public class InvalidTemplateFieldException extends ValidationException {
    public InvalidTemplateFieldException(String reason) {
        super(ErrorCode.INVALID_TEMPLATE_FIELD, 
            reason);
    }
    public InvalidTemplateFieldException(UUID templateId) {
        super(ErrorCode.INVALID_TEMPLATE_FIELD, 
            "Invalid template id '" + templateId.toString() + "'.");
    }
}
