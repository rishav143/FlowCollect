package com.cashclarity.application.invoice;

import com.cashclarity.api.v1.invoice.dto.FollowUpRequest;
import com.cashclarity.domain.invoice.Invoice;
import com.cashclarity.domain.invoice.followup.FollowUp;
import com.cashclarity.domain.invoice.followup.FollowUpChannel;
import com.cashclarity.domain.invoice.followup.FollowUpStatus;
import com.cashclarity.domain.invoice.followup.FollowUpTriggerType;
import com.cashclarity.domain.template.Template;
import com.cashclarity.exception.followup.FollowUpNotFoundException;
import com.cashclarity.exception.followup.InvalidFollowUpFieldException;
import com.cashclarity.exception.invoice.InvalidInvoiceFieldException;
import com.cashclarity.exception.template.InvalidTemplateFieldException;
import com.cashclarity.exception.template.TemplateNotFoundException;
import com.cashclarity.infrastructure.persistence.invoice.FollowUpJpaRepository;
import com.cashclarity.infrastructure.persistence.invoice.InvoiceJpaRepository;
import com.cashclarity.infrastructure.persistence.template.TemplateJpaRepository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;

@Service
public class FollowUpService {

    private final FollowUpJpaRepository followUpRepository;
    private final InvoiceJpaRepository invoiceRepository;
    private final TemplateJpaRepository templateRepository;

    public FollowUpService(
            FollowUpJpaRepository followUpRepository,
            InvoiceJpaRepository invoiceRepository,
            TemplateJpaRepository templateRepository
    ) {
        this.followUpRepository = followUpRepository;
        this.invoiceRepository = invoiceRepository;
        this.templateRepository = templateRepository;
    }

    /**
     * Creates a follow-up for an invoice.
     * Validates invoice ownership and optional template.
     * Propagates detailed exceptions for easier debugging.
     */
    @Transactional
    public FollowUp createFollowUp(UUID invoiceId, FollowUpRequest request) {
        if (request == null) {
            throw new InvalidInvoiceFieldException("request must not be null");
        }

        Invoice invoice = InvoiceUtil.getInvoiceOrThrow(invoiceId, invoiceRepository);

        // channel required
        FollowUpChannel channel = request.getChannel();
        if (channel == null) {
            throw new InvalidFollowUpFieldException("channel must not be null");
        }

        // trigger type - default MANUAL if null
        FollowUpTriggerType triggerType = request.getTriggerType() == null ? FollowUpTriggerType.MANUAL : request.getTriggerType();

        // template - optional
        Template template = null;
        if (request.getTemplateId() != null) {
            template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new TemplateNotFoundException(request.getTemplateId()));
            if (!template.isActive()) {
                throw new InvalidInvoiceFieldException("template must not be null and must be active");
            }
        }

        FollowUp followUp = new FollowUp();
        followUp.setInvoice(invoice);
        followUp.setChannel(channel);
        followUp.setTriggerType(triggerType);
        followUp.setStatus(FollowUpStatus.PENDING);
        if (template != null) {
            followUp.setTemplate(template);
        }

        return followUpRepository.save(followUp);
    }

    /**
     * Gets a follow-up by id.
     * Validates invoice ownership.
     */
    public FollowUp getFollowUp(UUID invoiceId, UUID followUpId) {
        FollowUp followUp = getFollowUpOrThrow(invoiceId, followUpId);
        return followUp;
    }

    /**
     * Gets follow-ups for an invoice by status, trigger type, and channel.
     */
    public Page<FollowUp> getFollowUps(
        UUID invoiceId,
        FollowUpStatus status, 
        FollowUpTriggerType triggerType, 
        FollowUpChannel channel, 
        Pageable pageable
    ) {

        InvoiceUtil.getInvoiceOrThrow(invoiceId, invoiceRepository);

        Specification<FollowUp> spec = (root, query, cb) -> {
            Predicate p = cb.equal(root.get("invoice").get("id"), invoiceId);
            if (status != null) {
                p = cb.and(p, cb.equal(root.get("status"), status));
            }
            if (triggerType != null) {
                p = cb.and(p, cb.equal(root.get("triggerType"), triggerType));
            }
            if (channel != null) {
                p = cb.and(p, cb.equal(root.get("channel"), channel));
            }
            return p;
        };

        return followUpRepository.findAll(spec, pageable);
    }

    /**
     * Updates a follow-up.
     * Updates the channel, trigger type, and template.
     */
    public FollowUp updateFollowUp(UUID invoiceId, UUID followUpId, FollowUpRequest request) {
        if (request == null) {
            throw new InvalidFollowUpFieldException("request must not be null");
        }

        FollowUp followUp = getFollowUpOrThrow(invoiceId, followUpId);

        if (request.getChannel() != null) {
            followUp.setChannel(request.getChannel());
        }

        if (request.getTriggerType() != null) {
            followUp.setTriggerType(request.getTriggerType());
        }

        if (request.getTemplateId() != null) {
            Template template = templateRepository.findById(request.getTemplateId())
                    .orElseThrow(() -> new TemplateNotFoundException(request.getTemplateId()));
            if (!template.isActive()) {
                throw new InvalidTemplateFieldException("template must not be null and must be active");
            }
        }
        
        return followUpRepository.save(followUp);
    }

    /**
     * Sends a follow-up.
     * Sets the status to SENT and the sentAt to the current time.
     */
    public FollowUp sendFollowUp(UUID invoiceId, UUID followUpId) {
        FollowUp followUp = getFollowUpOrThrow(invoiceId, followUpId);
        followUp.send();
        return followUpRepository.save(followUp);
    }

    /**
     * Fails a follow-up.
     * Sets the status to FAILED.
     */
    public FollowUp failFollowUp(UUID invoiceId, UUID followUpId) {
        FollowUp followUp = getFollowUpOrThrow(invoiceId, followUpId);
        followUp.fail();
        return followUpRepository.save(followUp);
    }

    /* ======================
       FollowUp Internal Service methods
       ====================== */

    private FollowUp getFollowUpOrThrow(UUID invoiceId, UUID followUpId) {
        FollowUp followUp = followUpRepository.findById(followUpId)
                .orElseThrow(() -> new FollowUpNotFoundException(followUpId));
        validateFollowUp(invoiceId, followUp);
        return followUp;
    }

    private void validateFollowUp(UUID invoiceId, FollowUp followUp) {
        if (followUp.getInvoice().getId() != invoiceId) {
            throw new InvalidFollowUpFieldException("follow-up must belong to invoice");
        }
    }
}

