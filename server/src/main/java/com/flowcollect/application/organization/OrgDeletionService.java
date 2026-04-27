package com.flowcollect.application.organization;

import com.flowcollect.exception.http.NotFoundException;
import com.flowcollect.infrastructure.persistence.organization.OrganizationJpaRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Hard-deletes an organization and every row it owns, in FK-safe order.
 *
 * All statements run inside a single transaction — either everything is
 * deleted or nothing is, so there is no risk of half-wiped data.
 *
 * Deletion order (child-first, respecting FK constraints):
 *   follow_ups → payment_confirmations → confirmation_links → payment_links
 *   → payments → invoice_items → invoice_tax_lines → invoices → customers
 *   → reminder_rule_occurrence_templates → reminder_rules → templates
 *   → ai_insight_cache → organization_invites → phone_otps
 *   → organization_gateway_configs → org_payment_details
 *   → email_verification_tokens → password_reset_tokens → user_oauth_connections
 *   → users → organizations
 */
@Service
public class OrgDeletionService {

    private static final Logger log = LoggerFactory.getLogger(OrgDeletionService.class);

    private final EntityManager em;
    private final OrganizationJpaRepository organizationRepository;

    public OrgDeletionService(EntityManager em, OrganizationJpaRepository organizationRepository) {
        this.em = em;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public void deleteOrganizationAndAllData(UUID orgId) {
        if (!organizationRepository.existsById(orgId)) {
            throw new NotFoundException("Organization not found: " + orgId);
        }

        log.info("[org={}] Starting hard delete — purging all data", orgId);

        // ── Invoice graph ────────────────────────────────────────────────────

        int followUps = jpql("DELETE FROM FollowUp f WHERE f.invoice.organization.id = :id", orgId);
        log.debug("[org={}] Deleted {} follow_ups", orgId, followUps);

        int paymentConfirmations = jpql(
            "DELETE FROM PaymentConfirmation pc WHERE pc.confirmationLink.invoice.organization.id = :id", orgId);
        log.debug("[org={}] Deleted {} payment_confirmations", orgId, paymentConfirmations);

        int confirmationLinks = jpql(
            "DELETE FROM ConfirmationLink cl WHERE cl.invoice.organization.id = :id", orgId);
        log.debug("[org={}] Deleted {} confirmation_links", orgId, confirmationLinks);

        int paymentLinks = jpql(
            "DELETE FROM PaymentLink pl WHERE pl.invoice.organization.id = :id", orgId);
        log.debug("[org={}] Deleted {} payment_links", orgId, paymentLinks);

        int payments = jpql("DELETE FROM Payment p WHERE p.invoice.organization.id = :id", orgId);
        log.debug("[org={}] Deleted {} payments", orgId, payments);

        // invoice_items and invoice_tax_lines have CascadeType.ALL on Invoice,
        // but JPQL bulk DELETE bypasses JPA cascade — must delete manually.
        int invoiceItems = jpql(
            "DELETE FROM InvoiceItem ii WHERE ii.invoice.organization.id = :id", orgId);
        log.debug("[org={}] Deleted {} invoice_items", orgId, invoiceItems);

        int taxLines = jpql(
            "DELETE FROM InvoiceTaxLine itl WHERE itl.invoice.organization.id = :id", orgId);
        log.debug("[org={}] Deleted {} invoice_tax_lines", orgId, taxLines);

        int invoices = jpql("DELETE FROM Invoice i WHERE i.organization.id = :id", orgId);
        log.debug("[org={}] Deleted {} invoices", orgId, invoices);

        // ── Customer ─────────────────────────────────────────────────────────

        int customers = jpql("DELETE FROM Customer c WHERE c.organization.id = :id", orgId);
        log.debug("[org={}] Deleted {} customers", orgId, customers);

        // ── Reminder / Template ──────────────────────────────────────────────

        // @ElementCollection is not a JPA entity — must use native SQL.
        int occurrenceTemplates = native_(
            "DELETE FROM reminder_rule_occurrence_templates " +
            "WHERE reminder_rule_id IN (SELECT id FROM reminder_rules WHERE organization_id = :id)", orgId);
        log.debug("[org={}] Deleted {} reminder_rule_occurrence_templates", orgId, occurrenceTemplates);

        int rules = jpql("DELETE FROM ReminderRule r WHERE r.organization.id = :id", orgId);
        log.debug("[org={}] Deleted {} reminder_rules", orgId, rules);

        int templates = jpql("DELETE FROM Template t WHERE t.organization.id = :id", orgId);
        log.debug("[org={}] Deleted {} templates", orgId, templates);

        // ── Org-scoped tables ────────────────────────────────────────────────

        int aiCache = jpql("DELETE FROM AiInsightCache a WHERE a.organization.id = :id", orgId);
        log.debug("[org={}] Deleted {} ai_insight_cache", orgId, aiCache);

        int invites = jpql("DELETE FROM OrganizationInvite oi WHERE oi.organization.id = :id", orgId);
        log.debug("[org={}] Deleted {} organization_invites", orgId, invites);

        int phoneOtps = jpql("DELETE FROM PhoneOtp po WHERE po.organization.id = :id", orgId);
        log.debug("[org={}] Deleted {} phone_otps", orgId, phoneOtps);

        int gateways = jpql("DELETE FROM OrganizationGatewayConfig gc WHERE gc.organization.id = :id", orgId);
        log.debug("[org={}] Deleted {} organization_gateway_configs", orgId, gateways);

        // org_payment_details uses a plain org_id column, not a JPA entity FK.
        int paymentDetails = native_("DELETE FROM org_payment_details WHERE org_id = :id", orgId);
        log.debug("[org={}] Deleted {} org_payment_details", orgId, paymentDetails);

        // ── User graph ───────────────────────────────────────────────────────

        int emailTokens = jpql(
            "DELETE FROM EmailVerificationToken t WHERE t.user.organization.id = :id", orgId);
        log.debug("[org={}] Deleted {} email_verification_tokens", orgId, emailTokens);

        int resetTokens = jpql(
            "DELETE FROM PasswordResetToken t WHERE t.user.organization.id = :id", orgId);
        log.debug("[org={}] Deleted {} password_reset_tokens", orgId, resetTokens);

        int oauthConnections = jpql(
            "DELETE FROM UserOAuthConnection c WHERE c.user.organization.id = :id", orgId);
        log.debug("[org={}] Deleted {} user_oauth_connections", orgId, oauthConnections);

        int users = jpql("DELETE FROM User u WHERE u.organization.id = :id", orgId);
        log.debug("[org={}] Deleted {} users", orgId, users);

        // ── Organization ─────────────────────────────────────────────────────

        organizationRepository.deleteById(orgId);
        log.info("[org={}] Hard delete complete", orgId);
    }

    private int jpql(String jpql, UUID orgId) {
        return em.createQuery(jpql).setParameter("id", orgId).executeUpdate();
    }

    @SuppressWarnings("unchecked")
    private int native_(String sql, UUID orgId) {
        return em.createNativeQuery(sql).setParameter("id", orgId).executeUpdate();
    }
}
