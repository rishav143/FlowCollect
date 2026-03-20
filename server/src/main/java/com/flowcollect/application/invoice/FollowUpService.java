package com.flowcollect.application.invoice;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.flowcollect.api.v1.invoice.dto.FollowUpRequest;
import com.flowcollect.api.v1.invoice.dto.MultiChannelFollowUpRequest;
import com.flowcollect.application.confirmation.ConfirmationLinkService;
import com.flowcollect.application.paymentlink.PaymentLinkService;
import com.flowcollect.application.reminder.NotificationSender;
import com.flowcollect.application.template.TemplateRenderer;
import com.flowcollect.application.template.TemplateService;
import com.flowcollect.domain.confirmation.ConfirmationLink;
import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.followup.FollowUp;
import com.flowcollect.domain.organization.PaymentCollectionMode;
import com.flowcollect.domain.invoice.followup.FollowUpChannel;
import com.flowcollect.domain.invoice.followup.FollowUpStatus;
import com.flowcollect.domain.invoice.followup.FollowUpTriggerType;
import com.flowcollect.domain.invoice.paymentlink.PaymentGateway;
import com.flowcollect.domain.invoice.paymentlink.PaymentLink;
import com.flowcollect.domain.template.Template;
import com.flowcollect.domain.template.TemplateChannel;
import com.flowcollect.exception.http.InternalException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.invoice.FollowUpJpaRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class FollowUpService {

    private static final Logger log = LoggerFactory.getLogger(FollowUpService.class);

    private final FollowUpJpaRepository followUpRepository;
    private final InvoiceService invoiceService;
    private final TemplateService templateService;
    private final TemplateRenderer templateRenderer;
    private final PaymentLinkService paymentLinkService;
    private final ConfirmationLinkService confirmationLinkService;
    private final Map<FollowUpChannel, NotificationSender> notificationSenders;

    public FollowUpService(
            FollowUpJpaRepository followUpRepository,
            InvoiceService invoiceService,
            TemplateService templateService,
            TemplateRenderer templateRenderer,
            PaymentLinkService paymentLinkService,
            ConfirmationLinkService confirmationLinkService,
            List<NotificationSender> notificationSenders
    ) {
        this.followUpRepository = followUpRepository;
        this.invoiceService = invoiceService;
        this.templateService = templateService;
        this.templateRenderer = templateRenderer;
        this.paymentLinkService = paymentLinkService;
        this.confirmationLinkService = confirmationLinkService;
        this.notificationSenders = indexSenders(notificationSenders);
    }

    @Transactional(readOnly = true)
    public boolean existsByInvoiceIdAndReminderRuleIdAndOccurrenceIndex(
            UUID invoiceId, UUID reminderRuleId, int occurrenceIndex) {
        if (invoiceId == null || reminderRuleId == null) {
            return false;
        }
        return followUpRepository.existsByInvoiceIdAndReminderRuleIdAndOccurrenceIndex(invoiceId, reminderRuleId, occurrenceIndex);
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

    /**
     * Marks a follow-up as CANCELLED and commits the change in an isolated transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FollowUp cancelFollowUp(FollowUp followUp) {
        if (followUp == null) {
            return null;
        }
        FollowUp fresh = followUpRepository.findById(followUp.getId()).orElse(null);
        if (fresh == null || !fresh.isPending()) {
            return followUp;
        }
        fresh.cancel();
        return followUpRepository.save(fresh);
    }

    // Create a follow-up for an invoice.
    @Transactional
    public FollowUp createFollowUp(UUID organizationId, UUID invoiceId, FollowUpRequest request) {
        if (request == null) {
            throw new ValidationException("Follow-up request must not be null");
        }
        Invoice invoice = invoiceService.getInvoiceById(organizationId, invoiceId);

        FollowUpChannel channel = request.getChannel();
        if (channel == null) {
            throw new ValidationException("Follow-up channel must not be null");
        }

        FollowUpTriggerType triggerType = request.getTriggerType() == null
                ? FollowUpTriggerType.MANUAL
                : request.getTriggerType();

        Template template = null;
        if (request.getTemplateId() != null) {
            template = templateService.getTemplateById(request.getTemplateId());
            if (!template.isActive()) {
                throw new ValidationException("Template with ID " + request.getTemplateId() + " is not active");
            }
        }

        FollowUp followUp = new FollowUp();
        followUp.setInvoice(invoice);
        followUp.setChannel(channel);
        followUp.setTriggerType(triggerType);
        followUp.setStatus(FollowUpStatus.PENDING);
        if (request.getScheduledForDate() != null) {
            followUp.setScheduledForDate(request.getScheduledForDate());
        } else if (triggerType == FollowUpTriggerType.MANUAL) {
            followUp.setScheduledForDate(LocalDate.now(invoice.getOrganization().getTimezone()));
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
     */
    @Transactional
    public List<FollowUp> createAndDispatchFollowUps(UUID organizationId, UUID invoiceId, MultiChannelFollowUpRequest request) {
        if (request == null) {
            throw new ValidationException("Multi-channel follow-up request must not be null");
        }
        if (request.getChannels() == null || request.getChannels().isEmpty()) {
            throw new ValidationException("At least one follow-up channel must be provided");
        }

        List<FollowUpChannel> channels = request.getChannels()
                .stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (channels.isEmpty()) {
            throw new ValidationException("At least one non-null follow-up channel must be provided");
        }

        if (request.isIncludePaymentLink() && request.getPaymentGateway() == null) {
            throw new ValidationException("paymentGateway is required when includePaymentLink is true");
        }

        Invoice invoice = invoiceService.getInvoiceById(organizationId, invoiceId);

        PaymentLink paymentLink = null;
        if (request.isIncludePaymentLink()) {
            PaymentGateway gateway = request.getPaymentGateway();
            paymentLink = paymentLinkService.createPaymentLink(invoice, gateway);
        }

        List<FollowUp> createdAndDispatched = new java.util.ArrayList<>(channels.size());
        for (FollowUpChannel channel : channels) {
            FollowUpRequest singleRequest = new FollowUpRequest();
            singleRequest.setChannel(channel);
            singleRequest.setTriggerType(FollowUpTriggerType.MANUAL);
            singleRequest.setTemplateId(request.getTemplateId());
            singleRequest.setScheduledForDate(request.getScheduledForDate());
            singleRequest.setAttachPdf(request.getAttachPdf());

            FollowUp followUp = createFollowUp(organizationId, invoiceId, singleRequest);
            if (paymentLink != null) {
                followUp.setPaymentLink(paymentLink);
                followUp = followUpRepository.save(followUp);
            }
            FollowUp dispatched = dispatchFollowUp(followUp);
            createdAndDispatched.add(dispatched);
        }

        return createdAndDispatched;
    }

    // Get a follow-up by id. Validates invoice ownership.
    public FollowUp getFollowUp(UUID organizationId, UUID invoiceId, UUID followUpId) {
        invoiceService.getInvoiceById(organizationId, invoiceId);
        return InvoiceUtil.getFollowUpOrThrow(invoiceId, followUpId, followUpRepository);
    }

    // Get follow-ups for an invoice filtered by status, trigger type, and channel.
    public Page<FollowUp> getFollowUps(
            UUID organizationId,
            UUID invoiceId,
            FollowUpStatus status,
            FollowUpTriggerType triggerType,
            FollowUpChannel channel,
            Pageable pageable
    ) {
        invoiceService.getInvoiceById(organizationId, invoiceId);

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

    // Update a PENDING follow-up's channel, template, scheduled date, or attachPdf.
    @Transactional
    public FollowUp updateFollowUp(UUID organizationId, UUID invoiceId, UUID followUpId, FollowUpRequest request) {
        if (request == null) {
            throw new ValidationException("Request must not be null");
        }
        invoiceService.getInvoiceById(organizationId, invoiceId);
        FollowUp followUp = InvoiceUtil.getFollowUpOrThrow(invoiceId, followUpId, followUpRepository);

        if (!followUp.isPending()) {
            throw new ValidationException("Only PENDING follow-ups can be updated. Current status: " + followUp.getStatus());
        }

        if (request.getChannel() != null) {
            followUp.setChannel(request.getChannel());
        }
        if (request.getTriggerType() != null) {
            followUp.setTriggerType(request.getTriggerType());
        }
        if (request.getTemplateId() != null) {
            Template template = templateService.getTemplateById(request.getTemplateId());
            if (!template.isActive()) {
                throw new ValidationException("Template must be active");
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
     */
    @Transactional
    public FollowUp dispatchFollowUp(UUID organizationId, UUID invoiceId, UUID followUpId) {
        invoiceService.getInvoiceById(organizationId, invoiceId);
        FollowUp followUp = InvoiceUtil.getFollowUpOrThrow(invoiceId, followUpId, followUpRepository);
        return dispatchFollowUp(followUp);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public FollowUp dispatchFollowUp(FollowUp followUp) {
        if (followUp == null) {
            return null;
        }

        FollowUp fresh = followUpRepository.findById(followUp.getId()).orElse(null);
        if (fresh == null || !fresh.isPending()) {
            return followUp;
        }

        try {
            Customer customer = requireDispatchableCustomer(fresh);
            Template template = requireDispatchableTemplate(fresh);
            String subject = templateRenderer.renderSubject(template, fresh.getInvoice(), customer);

            String paymentLinkUrl = fresh.getPaymentLink() != null
                    ? fresh.getPaymentLink().getPublicUrl()
                    : null;

            String confirmationLinkUrl = null;
            if (fresh.getInvoice().getOrganization().getPaymentCollectionMode()
                    == PaymentCollectionMode.CONFIRMATION_FLOW) {
                ConfirmationLink confirmationLink =
                        confirmationLinkService.getOrCreateForInvoice(fresh.getInvoice());
                confirmationLinkUrl = confirmationLink.getPublicUrl();
            }

            String body = templateRenderer.renderBody(
                    template, fresh.getInvoice(), customer, paymentLinkUrl, confirmationLinkUrl);

            NotificationSender sender = resolveSender(fresh.getChannel());
            sender.send(customer, subject, body, fresh.isAttachPdf(), fresh.getInvoice());

            fresh.send();
            return followUpRepository.save(fresh);
        } catch (RuntimeException ex) {
            log.warn("[followUp={}] Dispatch failed — marking FAILED", fresh.getId(), ex);
            fresh.fail();
            return followUpRepository.save(fresh);
        }
    }

    // Manually marks a follow-up as FAILED.
    @Transactional
    public FollowUp failFollowUp(UUID organizationId, UUID invoiceId, UUID followUpId) {
        invoiceService.getInvoiceById(organizationId, invoiceId);
        FollowUp followUp = InvoiceUtil.getFollowUpOrThrow(invoiceId, followUpId, followUpRepository);
        followUp.fail();
        return followUpRepository.save(followUp);
    }

    // Delete a PENDING or FAILED follow-up.
    @Transactional
    public void deleteFollowUp(UUID organizationId, UUID invoiceId, UUID followUpId) {
        invoiceService.getInvoiceById(organizationId, invoiceId);
        FollowUp followUp = InvoiceUtil.getFollowUpOrThrow(invoiceId, followUpId, followUpRepository);
        if (followUp.isSent()) {
            throw new ValidationException("Cannot delete a follow-up that has already been sent");
        }
        followUpRepository.delete(followUp);
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
        if (!invoice.isIssued() && !invoice.isPartiallyPaid()) {
            throw new ValidationException("Follow-ups can only be dispatched for issued or partially paid invoices");
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
