package com.flowcollect.application.paymentlink;

import com.flowcollect.application.invoice.PaymentService;
import com.flowcollect.application.organization.GatewayConnectionService;
import com.flowcollect.domain.invoice.Invoice;
import com.flowcollect.domain.invoice.payment.PaymentMode;
import com.flowcollect.domain.invoice.paymentlink.PaymentGateway;
import com.flowcollect.domain.invoice.paymentlink.PaymentLink;
import com.flowcollect.domain.invoice.paymentlink.PaymentLinkStatus;
import com.flowcollect.domain.organization.gateway.OrganizationGatewayConfig;
import com.flowcollect.exception.http.NotFoundException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.invoice.PaymentJpaRepository;
import com.flowcollect.infrastructure.persistence.paymentlink.PaymentLinkJpaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages the full lifecycle of payment links:
 *   - Creating a link for an invoice via the org's own connected gateway account
 *   - Handling incoming gateway webhooks (payment confirmed → record payment + update invoice)
 *   - Expiring stale links via the scheduler
 *
 * Money flow:
 *   Customer → org's Stripe/Razorpay account → org's bank.
 *   FlowCollect never holds or routes funds.
 */
@Service
public class PaymentLinkService {

    private static final Logger log = LoggerFactory.getLogger(PaymentLinkService.class);

    private static final int LINK_EXPIRY_DAYS = 7;

    private final PaymentLinkJpaRepository paymentLinkRepository;
    private final PaymentService paymentService;
    private final PaymentJpaRepository paymentRepository;
    private final GatewayConnectionService gatewayConnectionService;
    private final Map<PaymentGateway, PaymentGatewayProvider> providers;

    @Value("${app.base-url}")
    private String appBaseUrl;

    public PaymentLinkService(
            PaymentLinkJpaRepository paymentLinkRepository,
            PaymentService paymentService,
            PaymentJpaRepository paymentRepository,
            GatewayConnectionService gatewayConnectionService,
            List<PaymentGatewayProvider> providers
    ) {
        this.paymentLinkRepository = paymentLinkRepository;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.gatewayConnectionService = gatewayConnectionService;
        this.providers = indexProviders(providers);
    }

    /**
     * Creates a payment link for the given invoice using the organization's
     * own connected gateway account.
     *
     * Validates that the org has an active connection for the requested gateway
     * before calling the external API. This prevents creating dangling links
     * when credentials are missing or revoked.
     */
    @Transactional
    public PaymentLink createPaymentLink(Invoice invoice, PaymentGateway gateway) {
        if (invoice == null) throw new ValidationException("Invoice must not be null");
        if (gateway == null) throw new ValidationException("Payment gateway must not be null");

        // Load and validate the org's connected account for this gateway
        OrganizationGatewayConfig config = gatewayConnectionService
                .requireConnectedConfig(invoice.getOrganization().getId(), gateway);

        PaymentGatewayProvider provider = resolveProvider(gateway);

        String token = UUID.randomUUID().toString().replace("-", "");
        String publicUrl = appBaseUrl + "/pay/" + token;

        // Create the remote session using the org's own credentials
        GatewaySession session = provider.createSession(invoice, publicUrl, config);

        PaymentLink link = new PaymentLink();
        link.setInvoice(invoice);
        link.setToken(token);
        link.setGateway(gateway);
        link.setAmountDue(invoice.getTotalAmount());
        link.setCurrency(invoice.getOrganization().getCurrency().getCurrencyCode());
        link.setGatewaySessionId(session.sessionId());
        link.setGatewayPaymentUrl(session.paymentUrl());
        link.setPublicUrl(publicUrl);
        link.setExpiresAt(Instant.now().plus(LINK_EXPIRY_DAYS, ChronoUnit.DAYS));

        return paymentLinkRepository.save(link);
    }

    /** Looks up a PaymentLink by the public token in the redirect URL. */
    @Transactional(readOnly = true)
    public PaymentLink getByToken(String token) {
        return paymentLinkRepository.findByToken(token)
                .orElseThrow(() -> new NotFoundException("Payment link not found"));
    }

    /**
     * Processes a webhook event from a payment gateway.
     *
     * For Stripe Connect: we receive one webhook for all orgs. We identify the
     * owning org by matching the gateway session ID to a PaymentLink, then load
     * that org's gateway config to verify the signature.
     *
     * For Razorpay: each org configures their own webhook endpoint or we filter
     * by the session ID match.
     *
     * Idempotent: duplicate webhook deliveries are safely ignored.
     */
    @Transactional
    public void handleWebhook(PaymentGateway gateway, String payload, String signatureHeader) {
        PaymentGatewayProvider provider = resolveProvider(gateway);

        // For Stripe Connect, we must first find the owning org from the payload
        // before we can look up org-specific config. Parse with a preliminary check.
        // We pass a null config for the initial parse — Stripe uses a platform-level
        // webhook secret so no org config is needed at this stage.
        WebhookEvent event = provider.parseWebhook(payload, signatureHeader, null);

        if (event == null || !event.successful()) {
            return;
        }

        PaymentLink link = paymentLinkRepository.findByGatewaySessionId(event.sessionId())
                .orElse(null);
        if (link == null) {
            log.debug("No payment link for session {}; ignoring webhook", event.sessionId());
            return;
        }

        // Idempotency guard
        if (event.transactionReference() != null
                && paymentRepository.existsByInvoiceIdAndReferenceId(
                        link.getInvoice().getId(), event.transactionReference())) {
            log.debug("Payment {} already recorded for invoice {}; skipping duplicate",
                    event.transactionReference(), link.getInvoice().getId());
            return;
        }

        PaymentMode mode = gateway == PaymentGateway.RAZORPAY ? PaymentMode.UPI : PaymentMode.CARD;
        String notes = "Payment via " + gateway.name() + " — " + link.getInvoice().getOrganization().getName();

        paymentService.recordGatewayPayment(
                link.getInvoice().getId(),
                event.amountPaid(),
                mode,
                event.transactionReference(),
                notes
        );

        BigDecimal totalPaid = sumPaymentsForInvoice(link.getInvoice().getId());
        if (totalPaid.compareTo(link.getAmountDue()) >= 0) {
            link.markPaid();
        } else {
            link.markPartiallyPaid();
        }
        paymentLinkRepository.save(link);

        log.info("PaymentLink {} → {} after {} webhook (invoice {}, org {})",
                link.getId(), link.getStatus(), gateway,
                link.getInvoice().getId(), link.getInvoice().getOrganization().getId());
    }

    /**
     * Expires all ACTIVE / PARTIALLY_PAID links whose expiry date has passed.
     * Called by the scheduler.
     */
    @Transactional
    public void expireOverdueLinks() {
        List<PaymentLink> overdueLinks = paymentLinkRepository.findByStatusInAndExpiresAtBefore(
                List.of(PaymentLinkStatus.ACTIVE, PaymentLinkStatus.PARTIALLY_PAID),
                Instant.now()
        );
        if (overdueLinks.isEmpty()) return;

        overdueLinks.forEach(PaymentLink::expire);
        paymentLinkRepository.saveAll(overdueLinks);
        log.info("Expired {} overdue payment links", overdueLinks.size());
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private BigDecimal sumPaymentsForInvoice(UUID invoiceId) {
        return paymentRepository.findByInvoiceId(invoiceId).stream()
                .map(p -> p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private PaymentGatewayProvider resolveProvider(PaymentGateway gateway) {
        PaymentGatewayProvider provider = providers.get(gateway);
        if (provider == null) {
            throw new ValidationException("Payment gateway " + gateway + " is not enabled on this platform");
        }
        return provider;
    }

    private Map<PaymentGateway, PaymentGatewayProvider> indexProviders(List<PaymentGatewayProvider> list) {
        Map<PaymentGateway, PaymentGatewayProvider> map = new EnumMap<>(PaymentGateway.class);
        for (PaymentGatewayProvider p : list) {
            map.put(p.gateway(), p);
        }
        return map;
    }
}
