package com.flowcollect.application.invoice;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flowcollect.api.v1.invoice.dto.FollowUpRequest;
import com.flowcollect.api.v1.invoice.dto.MultiChannelFollowUpRequest;
import com.flowcollect.application.reminder.NotificationSender;
import com.flowcollect.application.template.TemplateRenderer;
import com.flowcollect.application.template.TemplateService;
import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.followup.FollowUp;
import com.flowcollect.domain.invoice.followup.FollowUpChannel;
import com.flowcollect.domain.invoice.followup.FollowUpStatus;
import com.flowcollect.domain.invoice.followup.FollowUpTriggerType;
import com.flowcollect.domain.template.Template;
import com.flowcollect.domain.template.TemplateChannel;
import com.flowcollect.exception.http.InternalException;
import com.flowcollect.exception.http.NotFoundException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.invoice.FollowUpJpaRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class FollowUpService {

    private final FollowUpJpaRepository followUpRepository;
    private final InvoiceService invoiceService;
    private final TemplateService templateService;
    private final TemplateRenderer templateRenderer;
    private final Map<FollowUpChannel, NotificationSender> notificationSenders;

    public FollowUpService(
            FollowUpJpaRepository followUpRepository,
            InvoiceService invoiceService,
            TemplateService templateService,
            TemplateRenderer templateRenderer,
            List<NotificationSender> notificationSenders
    ) {
        this.followUpRepository = followUpRepository;
        this.invoiceService = invoiceService;
        this.templateService = templateService;
        this.templateRenderer = templateRenderer;
        this.notificationSenders = indexSenders(notificationSenders);
    }

    @Transactional(readOnly = true)
    public boolean existsByInvoiceIdAndReminderRuleIdAndScheduledForDate(
            UUID invoiceId, UUID reminderRuleId, LocalDate scheduledForDate) {
        if (invoiceId == null || reminderRuleId == null || scheduledForDate == null) {
            return false;
        }
        return followUpRepository.existsByInvoiceIdAndReminderRuleIdAndScheduledForDate(invoiceId, reminderRuleId, scheduledForDate);
    }

    @Transactional(readOnly = true)
    public List<FollowUp> getPendingAutomatedFollowUps(UUID organizationId) {
        if (organizationId == null) {
            return List.of();
        }
        return followUpRepository.findByStatusAndTriggerTypeAndInvoiceOrganizationId(
                FollowUpStatus.PENDING,
                FollowUpTriggerType.AUTOMATED,
                organizationId
        );
    }

    @Transactional
    public FollowUp save(FollowUp followUp) {
        if (followUp == null) {
            throw new ValidationException("Follow-up must not be null");
        }
        return followUpRepository.save(followUp);
    }

    // Create a follow-up for an invoice.
    @Transactional
    public FollowUp createFollowUp
    (
        UUID invoiceId, 
        FollowUpRequest request
    ) {
        if (request == null) {
            throw new ValidationException("Follow-up request must not be null. Invoice ID: " + invoiceId);
        }
        Invoice invoice = invoiceService.getInvoiceById(invoiceId);

        // channel required
        FollowUpChannel channel = request.getChannel();
        if (channel == null) {
            throw new ValidationException("Follow-up channel must not be null");
        }

        // trigger type - default MANUAL if null
        FollowUpTriggerType triggerType = request.getTriggerType() == null 
        ? FollowUpTriggerType.MANUAL 
        : request.getTriggerType();

        // template - optional
        Template template = null;
        if (request.getTemplateId() != null) {
            template = templateService.getTemplateById(request.getTemplateId());
            if (!template.isActive()) {
                throw new ValidationException("Template must be active. Template with ID: " + request.getTemplateId() + " is not active.");
            }
        }

        FollowUp followUp = new FollowUp();
        followUp.setInvoice(invoice);
        followUp.setChannel(channel);
        followUp.setTriggerType(triggerType);
        followUp.setStatus(FollowUpStatus.PENDING);
        if (request.getScheduledForDate() != null) {
            followUp.setScheduledForDate(request.getScheduledForDate());
        }
        if (request.getAttachPdf() != null) {
            followUp.setAttachPdf(request.getAttachPdf());
        }
        if (template != null) {
            followUp.setTemplate(template);
        }

        return followUpRepository.save(followUp);
    }

    /**
     * Creates and dispatches manual follow-ups for multiple channels.
     * One FollowUp is created per channel and dispatched via the existing dispatch logic.
     */
    @Transactional
    public List<FollowUp> createAndDispatchFollowUps(
            UUID invoiceId,
            MultiChannelFollowUpRequest request
    ) {
        if (request == null) {
            throw new ValidationException("Multi-channel follow-up request must not be null. Invoice ID: " + invoiceId);
        }
        if (request.getChannels() == null || request.getChannels().isEmpty()) {
            throw new ValidationException("At least one follow-up channel must be provided");
        }

        // Deduplicate while preserving order
        List<FollowUpChannel> channels = request.getChannels()
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (channels.isEmpty()) {
            throw new ValidationException("At least one non-null follow-up channel must be provided");
        }

        // Reuse existing single-channel creation logic for correctness
        List<FollowUp> createdAndDispatched = new java.util.ArrayList<>(channels.size());
        for (FollowUpChannel channel : channels) {
            FollowUpRequest singleRequest = new FollowUpRequest();
            singleRequest.setChannel(channel);
            singleRequest.setTriggerType(FollowUpTriggerType.MANUAL);
            singleRequest.setTemplateId(request.getTemplateId());
            singleRequest.setScheduledForDate(request.getScheduledForDate());
            singleRequest.setAttachPdf(request.getAttachPdf());

            FollowUp followUp = createFollowUp(invoiceId, singleRequest);
            FollowUp dispatched = dispatchFollowUp(followUp);
            createdAndDispatched.add(dispatched);
        }

        return createdAndDispatched;
    }

    // Get a follow-up by id. Validates invoice ownership.
    public FollowUp getFollowUp
    (
        UUID invoiceId, 
        UUID followUpId
    ) throws ValidationException, NotFoundException {
        invoiceService.getInvoiceById(invoiceId);
        return InvoiceUtil.getFollowUpOrThrow(invoiceId, followUpId, followUpRepository);
    }

    // Get follow-ups for an invoice by status, trigger type, and channel.
    public Page<FollowUp> getFollowUps(
        UUID invoiceId,
        FollowUpStatus status, 
        FollowUpTriggerType triggerType, 
        FollowUpChannel channel, 
        Pageable pageable
    ) {
        invoiceService.getInvoiceById(invoiceId);

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

    // Update a follow-up. Updates the channel, trigger type, and template.
    public FollowUp updateFollowUp
    (
            UUID invoiceId,
            UUID followUpId,
            FollowUpRequest request
    ) {
        if (request == null) {
            throw new ValidationException("Request must not be null");
        }
        invoiceService.getInvoiceById(invoiceId);
        FollowUp followUp = InvoiceUtil.getFollowUpOrThrow(invoiceId, followUpId, followUpRepository);

        if (request.getChannel() != null) {
            followUp.setChannel(request.getChannel());
        }
        if (request.getTriggerType() != null) {
            followUp.setTriggerType(request.getTriggerType());
        }
        if (request.getTemplateId() != null) {
            Template template = templateService.getTemplateById(request.getTemplateId());
            if (!template.isActive()) {
                throw new ValidationException("Template must not be null and must be active");
            }
            followUp.setTemplate(template);
        }
        if (request.getScheduledForDate() != null) {
            followUp.setScheduledForDate(request.getScheduledForDate());
        }
        if (request.getAttachPdf() != null) {
            followUp.setAttachPdf(request.getAttachPdf());
        }
        
        return followUpRepository.save(followUp);
    }

    /**
     * Dispatches (delivers) a follow-up via its configured channel, then marks it SENT or FAILED.
     * This is used by both automated schedulers and manual API operations to ensure consistent logic.
     */
    @Transactional
    public FollowUp dispatchFollowUp(UUID invoiceId, UUID followUpId) {
        invoiceService.getInvoiceById(invoiceId);
        FollowUp followUp = InvoiceUtil.getFollowUpOrThrow(invoiceId, followUpId, followUpRepository);
        return dispatchFollowUp(followUp);
    }

    @Transactional
    public FollowUp dispatchFollowUp(FollowUp followUp) {
        if (followUp == null || !followUp.isPending()) {
            return followUp;
        }

        try {
            Customer customer = requireDispatchableCustomer(followUp);
            Template template = requireDispatchableTemplate(followUp);
            String subject = templateRenderer.renderSubject(template, followUp.getInvoice(), customer);
            String body = templateRenderer.renderBody(template, followUp.getInvoice(), customer);
            NotificationSender sender = resolveSender(followUp.getChannel());
            sender.send(customer, subject, body, followUp.isAttachPdf(), followUp.getInvoice());

            followUp.send();
            return followUpRepository.save(followUp);
        } catch (RuntimeException ex) {
            followUp.fail();
            followUpRepository.save(followUp);
            throw ex;
        }
    }

    // Mark a follow-up as SENT without dispatching (rare; prefer dispatchFollowUp).
    public FollowUp sendFollowUp
    (
            UUID invoiceId,
            UUID followUpId
    ) {
        invoiceService.getInvoiceById(invoiceId);
        FollowUp followUp = InvoiceUtil.getFollowUpOrThrow(invoiceId, followUpId, followUpRepository);
        followUp.send();
        return followUpRepository.save(followUp);
    }

    // Fail a follow-up. Sets the status to FAILED.
    public FollowUp failFollowUp
    (
            UUID invoiceId,
            UUID followUpId
    ) {
        invoiceService.getInvoiceById(invoiceId);
        FollowUp followUp = InvoiceUtil.getFollowUpOrThrow(invoiceId, followUpId, followUpRepository);
        followUp.fail();
        return followUpRepository.save(followUp);
    }

    private NotificationSender resolveSender(FollowUpChannel channel) {
        NotificationSender sender = notificationSenders.get(channel);
        if (sender == null) {
            throw new InternalException("No notification sender configured for channel " + channel);
        }
        return sender;
    }

    private Customer requireDispatchableCustomer(FollowUp followUp) {
        Invoice invoice = Objects.requireNonNull(followUp.getInvoice(), "Follow-up invoice must not be null");
        if (!invoice.isIssued()) {
            throw new ValidationException("Only issued invoices can have follow-ups dispatched");
        }

        Customer customer = invoice.getCustomer();
        if (customer == null) {
            throw new ValidationException("Invoice customer must not be null");
        }
        if (!customer.isActive()) {
            throw new ValidationException("Invoice customer must be active");
        }
        return customer;
    }

    private Template requireDispatchableTemplate(FollowUp followUp) {
        Template template = followUp.getTemplate();
        if (template == null || !template.isActive()) {
            throw new ValidationException("Follow-up template must be active");
        }
        if (template.getChannel() != TemplateChannel.valueOf(followUp.getChannel().name())) {
            throw new ValidationException("Template channel must match follow-up channel");
        }
        return template;
    }

    private Map<FollowUpChannel, NotificationSender> indexSenders(List<NotificationSender> senders) {
        Map<FollowUpChannel, NotificationSender> indexedSenders = new EnumMap<>(FollowUpChannel.class);
        for (NotificationSender sender : senders) {
            NotificationSender existing = indexedSenders.putIfAbsent(sender.channel(), sender);
            if (existing != null) {
                throw new InternalException("Duplicate notification sender configured for channel " + sender.channel());
            }
        }
        return indexedSenders;
    }
}

