package com.paidpeace.api.v1.invoice;

import com.paidpeace.api.v1.invoice.dto.FollowUpResponse;
import com.paidpeace.domain.invoice.followup.FollowUp;

public class FollowUpMapper {
    public static FollowUpResponse toResponse(FollowUp f) {
        FollowUpResponse followUpResponse = new FollowUpResponse();
        followUpResponse.setId(f.getId());
        followUpResponse.setInvoiceId(f.getInvoice() != null ? f.getInvoice().getId() : null);
        followUpResponse.setChannel(f.getChannel());
        followUpResponse.setTriggerType(f.getTriggerType());
        followUpResponse.setStatus(f.getStatus());
        followUpResponse.setTemplateId(f.getTemplate() != null ? f.getTemplate().getId() : null);
        followUpResponse.setSentAt(f.getSentAt());
        followUpResponse.setCreatedAt(f.getCreatedAt());
        return followUpResponse;
    }
}

