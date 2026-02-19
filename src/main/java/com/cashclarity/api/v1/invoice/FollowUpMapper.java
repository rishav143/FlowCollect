package com.cashclarity.api.v1.invoice;

import com.cashclarity.api.v1.invoice.dto.FollowUpResponse;
import com.cashclarity.domain.invoice.followup.FollowUp;

public class FollowUpMapper {
    public static FollowUpResponse toResponse(FollowUp f) {
        FollowUpResponse r = new FollowUpResponse();
        r.setId(f.getId());
        r.setInvoiceId(f.getInvoice() != null ? f.getInvoice().getId() : null);
        r.setChannel(f.getChannel());
        r.setTriggerType(f.getTriggerType());
        r.setStatus(f.getStatus());
        r.setTemplateId(f.getTemplate() != null ? f.getTemplate().getId() : null);
        r.setSentAt(f.getSentAt());
        r.setCreatedAt(f.getCreatedAt());
        return r;
    }
}

