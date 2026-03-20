package com.flowcollect.api.v1.invoice;

import com.flowcollect.api.v1.invoice.dto.FollowUpResponse;
import com.flowcollect.domain.invoice.followup.FollowUp;

public class FollowUpMapper {
    public static FollowUpResponse toResponse(FollowUp f) {
        FollowUpResponse followUpResponse = new FollowUpResponse();
        followUpResponse.setId(f.getId());
        followUpResponse.setInvoiceId(f.getInvoice() != null ? f.getInvoice().getId() : null);
        followUpResponse.setChannel(f.getChannel());
        followUpResponse.setTriggerType(f.getTriggerType());
        followUpResponse.setStatus(f.getStatus());
        followUpResponse.setTemplateId(f.getTemplate() != null ? f.getTemplate().getId() : null);
        followUpResponse.setReminderRuleId(f.getReminderRule() != null ? f.getReminderRule().getId() : null);
        followUpResponse.setScheduledForDate(f.getScheduledForDate());
        followUpResponse.setAttachPdf(f.isAttachPdf());
        followUpResponse.setSentAt(f.getSentAt());
        followUpResponse.setCreatedAt(f.getCreatedAt());
        followUpResponse.setUpdatedAt(f.getUpdatedAt());

        if (f.getPaymentLink() != null) {
            followUpResponse.setPaymentLinkId(f.getPaymentLink().getId());
            followUpResponse.setPaymentLinkUrl(f.getPaymentLink().getPublicUrl());
            followUpResponse.setPaymentLinkGateway(f.getPaymentLink().getGateway());
            followUpResponse.setPaymentLinkStatus(f.getPaymentLink().getStatus());
        }

        return followUpResponse;
    }
}

