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

import com.flowcollect.api.v1.invoice.dto.ConsolidatedFollowUpRequest;
import com.flowcollect.api.v1.invoice.dto.ConsolidatedFollowUpResponse;
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
import com.flowcollect.domain.invoice.followup.ClickedLinkType;
import com.flowcollect.domain.invoice.followup.DeliveryStatus;
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
    private final String appBaseUrl;

    public FollowUpService(
            FollowUpJpaRepository followUpRepository,
            InvoiceService invoiceService,
            TemplateService templateService,
            TemplateRenderer templateRenderer,
            PaymentLinkService paymentLinkService,
            ConfirmationLinkService confirmationLinkService,
            List<NotificationSender> notificationSenders,
            @org.springframework.beans.factory.annotation.Value("${app.base-url}") String appBaseUrl
    ) {
        this.followUpRepository = followUpRepository;
        this.invoiceService = invoiceService;
        this.templateService = templateService;
        this.templateRenderer = templateRenderer;
        this.paymentLinkService = paymentLinkService;
        this.confirmationLinkService = confirmationLinkService;
        this.notificationSenders = indexSenders(notificationSenders);
        this.appBaseUrl = appBaseUrl;
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
     * Cancels all PENDING follow-ups for the given invoice. Called when an invoice is cancelled.
     */
    @Transactional
    public void cancelPendingFollowUpsForInvoice(UUID invoiceId) {
        if (invoiceId == null) return;
        List<FollowUp> pending = followUpRepository.findByInvoiceIdAndStatus(invoiceId, FollowUpStatus.PENDING);
        for (FollowUp followUp : pending) {
            followUp.cancel();
            followUpRepository.save(followUp);
        }
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

        // Validate upfront — fail fast before creating any follow-up records
        Customer dispatchCustomer = invoice.getCustomer();
        if (dispatchCustomer == null) {
            throw new ValidationException(
                "Cannot send follow-up: no client is assigned to this invoice. Please assign a client first.");
        }
        if (!dispatchCustomer.isActive()) {
            throw new ValidationException(
                "Cannot send follow-up: the client assigned to this invoice is inactive.");
        }

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

    /**
     * Sends one consolidated message per channel to a customer covering multiple invoices.
     *
     * <p>One {@link FollowUp} record is created per invoice per channel for full audit trail.
     * However, only a single outbound message is sent per channel — rendered against all invoices
     * with PDFs (one per invoice) attached when requested.
     *
     * <p>All invoices must belong to the same customer and organization.
     */
    @Transactional
    public List<ConsolidatedFollowUpResponse> consolidatedDispatch(
            UUID organizationId, ConsolidatedFollowUpRequest request) {

        if (request == null) {
            throw new ValidationException("Consolidated follow-up request must not be null");
        }
        if (request.getInvoiceIds() == null || request.getInvoiceIds().isEmpty()) {
            throw new ValidationException("At least one invoice ID must be provided");
        }
        if (request.getChannels() == null || request.getChannels().isEmpty()) {
            throw new ValidationException("At least one channel must be provided");
        }
        // Resolve + validate all invoices
        List<Invoice> invoices = new java.util.ArrayList<>();
        for (UUID invoiceId : request.getInvoiceIds().stream().distinct().toList()) {
            invoices.add(invoiceService.getInvoiceById(organizationId, invoiceId));
        }

        // All must belong to the same customer
        UUID customerId = invoices.get(0).getCustomer() != null
                ? invoices.get(0).getCustomer().getId() : null;
        if (customerId == null) {
            throw new ValidationException(
                "Cannot send consolidated follow-up: the first invoice has no client assigned.");
        }
        for (Invoice inv : invoices) {
            if (inv.getCustomer() == null || !inv.getCustomer().getId().equals(customerId)) {
                throw new ValidationException(
                    "All invoices must belong to the same client for a consolidated follow-up.");
            }
            if (!inv.isIssued() && !inv.isPartiallyPaid()) {
                throw new ValidationException(
                    "Invoice " + inv.getInvoiceNumber() + " is not in a dispatchable state (must be ISSUED or PARTIALLY_PAID).");
            }
        }

        Customer customer = invoices.get(0).getCustomer();
        if (!customer.isActive()) {
            throw new ValidationException("Cannot send consolidated follow-up: the client is inactive.");
        }

        // Optional custom template — when absent, the built-in consolidated template is used
        Template template = null;
        if (request.getTemplateId() != null) {
            template = templateService.getTemplateById(organizationId, request.getTemplateId());
            if (!template.isActive()) {
                throw new ValidationException("Template is not active.");
            }
        }

        List<FollowUpChannel> channels = request.getChannels().stream().distinct().toList();
        List<ConsolidatedFollowUpResponse> results = new java.util.ArrayList<>(channels.size());

        for (FollowUpChannel channel : channels) {
            // Validate custom template channel match (only when a template was provided)
            if (template != null &&
                    template.getChannel() != com.flowcollect.domain.template.TemplateChannel.valueOf(channel.name())) {
                throw new ValidationException(
                    "Template channel " + template.getChannel() + " does not match requested channel " + channel + ".");
            }

            // Render subject + body — use custom template when provided, built-in otherwise
            String subject;
            String body;
            if (template != null) {
                subject = templateRenderer.renderConsolidatedSubject(template, invoices, customer);
                body    = templateRenderer.renderConsolidatedBody(template, invoices, customer, null, null);
            } else {
                subject = templateRenderer.renderBuiltInConsolidatedSubject(channel, invoices, customer);
                body    = templateRenderer.renderBuiltInConsolidatedBody(channel, invoices, customer);
            }

            // Create one FollowUp record per invoice (audit trail)
            final Template finalTemplate = template;
            List<FollowUp> followUps = new java.util.ArrayList<>(invoices.size());
            for (Invoice inv : invoices) {
                FollowUp fu = new FollowUp();
                fu.setInvoice(inv);
                fu.setChannel(channel);
                fu.setTriggerType(FollowUpTriggerType.MANUAL);
                fu.setStatus(FollowUpStatus.PENDING);
                if (finalTemplate != null) fu.setTemplate(finalTemplate);
                fu.setAttachPdf(false);
                fu.setScheduledForDate(LocalDate.now(inv.getOrganization().getTimezone()));
                followUps.add(followUpRepository.save(fu));
            }

            // Send one actual message for the entire group
            ConsolidatedFollowUpResponse response = new ConsolidatedFollowUpResponse();
            response.setChannel(channel);
            response.setInvoiceIds(invoices.stream().map(Invoice::getId).toList());
            response.setFollowUpIds(followUps.stream().map(FollowUp::getId).toList());

            try {
                NotificationSender sender = resolveSender(channel);
                String externalId = sender.sendConsolidated(customer, subject, body, invoices);

                java.time.Instant now = java.time.Instant.now();
                for (FollowUp fu : followUps) {
                    fu.send();
                    if (externalId != null) {
                        if (fu.getChannel() == FollowUpChannel.EMAIL) {
                            fu.setResendEmailId(externalId);
                        } else {
                            fu.setExternalChannelMessageId(externalId);
                        }
                    }
                    followUpRepository.save(fu);
                }

                response.setStatus(FollowUpStatus.SENT);
                response.setExternalMessageId(externalId);
                response.setSentAt(now);
                log.info("[consolidated] Sent {} to customer={} invoices={} externalId={}",
                        channel, customer.getId(), invoices.size(), externalId);

            } catch (RuntimeException ex) {
                log.warn("[consolidated] Dispatch failed channel={} customer={}", channel, customer.getId(), ex);
                for (FollowUp fu : followUps) {
                    fu.fail();
                    followUpRepository.save(fu);
                }
                response.setStatus(FollowUpStatus.FAILED);
                response.setErrorMessage(ex.getMessage());
            }

            results.add(response);
        }

        return results;
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

            // Wrap the relevant link through our tracking redirect (works for all channels)
            String trackingUrl = appBaseUrl + "/track/" + fresh.getId();

            // Only track links that the template actually uses — a template without
            // {{confirmationLink}} / {{paymentLink}} has nothing to click, so it must
            // not count towards click-rate metrics.
            String rawBody = template.getBody() != null ? template.getBody() : "";
            boolean templateHasConfirmationLink = rawBody.contains("{{confirmationLink}}");
            boolean templateHasPaymentLink      = rawBody.contains("{{paymentLink}}");

            if (confirmationLinkUrl != null && templateHasConfirmationLink) {
                fresh.setTrackedLinkUrl(confirmationLinkUrl);
                fresh.setClickedLinkType(ClickedLinkType.CONFIRMATION_LINK);
            } else if (paymentLinkUrl != null && templateHasPaymentLink) {
                fresh.setTrackedLinkUrl(paymentLinkUrl);
                fresh.setClickedLinkType(ClickedLinkType.PAYMENT_LINK);
            }

            // Replace the real URL with the tracking URL in the rendered message
            String effectivePaymentLinkUrl     = (paymentLinkUrl      != null && templateHasPaymentLink)      ? trackingUrl : null;
            String effectiveConfirmationLinkUrl = (confirmationLinkUrl != null && templateHasConfirmationLink) ? trackingUrl : null;

            String body = templateRenderer.renderBody(
                    template, fresh.getInvoice(), customer,
                    effectivePaymentLinkUrl, effectiveConfirmationLinkUrl);

            NotificationSender sender = resolveSender(fresh.getChannel());
            String externalMessageId = sender.send(customer, subject, body, fresh.isAttachPdf(), fresh.getInvoice());

            fresh.send();
            if (externalMessageId != null) {
                if (fresh.getChannel() == FollowUpChannel.EMAIL) {
                    fresh.setResendEmailId(externalMessageId);
                } else {
                    // SMS (Twilio SID) or WhatsApp (Meta message ID)
                    fresh.setExternalChannelMessageId(externalMessageId);
                }
            }
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

    /**
     * Called by provider webhooks (Twilio for SMS, Meta for WhatsApp) to record
     * async delivery confirmation or failure on a SENT follow-up.
     *
     * @param externalMessageId Twilio SID or Meta message ID
     * @param status            DELIVERED or UNDELIVERED
     */
    @Transactional
    public void updateDeliveryStatus(String externalMessageId, DeliveryStatus status) {
        if (externalMessageId == null || externalMessageId.isBlank()) return;
        followUpRepository.findByExternalChannelMessageId(externalMessageId).ifPresent(fu -> {
            fu.setDeliveryStatus(status);
            followUpRepository.save(fu);
            log.info("[followUp={}] delivery status updated to {} (externalId={})",
                    fu.getId(), status, externalMessageId);
        });
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
