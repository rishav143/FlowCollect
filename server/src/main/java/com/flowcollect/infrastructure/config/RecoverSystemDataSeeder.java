package com.flowcollect.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.flowcollect.domain.reminder.RuleMode;
import com.flowcollect.domain.template.Template;
import com.flowcollect.domain.template.TemplateChannel;
import com.flowcollect.domain.template.TemplateTone;
import com.flowcollect.infrastructure.persistence.invoice.FollowUpJpaRepository;
import com.flowcollect.infrastructure.persistence.reminder.ReminderRuleJpaRepository;
import com.flowcollect.infrastructure.persistence.template.TemplateJpaRepository;

/**
 * Seeds platform-defined MANUAL default templates on startup.
 *
 * Idempotency: checks by name (system-scoped, org=null) before inserting — safe to run on every restart.
 * User edits (body, subject, active flag) are preserved across restarts because we only check existence,
 * never overwrite. Also performs one-time cleanup of legacy AUTO system templates/rules from earlier versions.
 */
@Component
public class RecoverSystemDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RecoverSystemDataSeeder.class);

    // Legacy names — kept only for cleanup, do not re-use.
    private static final String LEGACY_TPL_DAY3   = "Day 3 Recovery Nudge";
    private static final String LEGACY_TPL_DAY7   = "Day 7 Recovery Reminder";
    private static final String LEGACY_RULE_DAY3  = "System: 3-Day Overdue WhatsApp";
    private static final String LEGACY_RULE_DAY7  = "System: 7-Day Overdue Email";

    // Current default template names — used as idempotency keys.
    static final String TPL_DEFAULT_EMAIL     = "Default Email";
    static final String TPL_DEFAULT_SMS       = "Default SMS";
    static final String TPL_DEFAULT_WHATSAPP  = "Default WhatsApp";

    private final TemplateJpaRepository     templateRepository;
    private final ReminderRuleJpaRepository reminderRuleRepository;
    private final FollowUpJpaRepository     followUpRepository;

    public RecoverSystemDataSeeder(
            TemplateJpaRepository templateRepository,
            ReminderRuleJpaRepository reminderRuleRepository,
            FollowUpJpaRepository followUpRepository) {
        this.templateRepository     = templateRepository;
        this.reminderRuleRepository = reminderRuleRepository;
        this.followUpRepository     = followUpRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        cleanupLegacySystemData();

        ensureTemplate(
                TPL_DEFAULT_EMAIL,
                TemplateChannel.EMAIL,
                "Invoice {{invoiceNumber}} — Payment Confirmation",
                "Dear {{customerName}},\n\n" +
                "This is a reminder that invoice {{invoiceNumber}} for {{remainingAmount}} is due on {{dueDate}}.\n\n" +
                "If you have already made the payment, please click the button below to confirm.\n\n" +
                "{{confirmationLink}}\n\n" +
                "Regards,\n{{organizationName}}"
        );

        ensureTemplate(
                TPL_DEFAULT_SMS,
                TemplateChannel.SMS,
                null,
                "Hi {{customerName}}, invoice {{invoiceNumber}} for {{remainingAmount}} is due on {{dueDate}}. " +
                "Confirm receipt: {{confirmationLink}}"
        );

        ensureTemplate(
                TPL_DEFAULT_WHATSAPP,
                TemplateChannel.WHATSAPP,
                null,
                "Hi {{customerName}}, this is a reminder for invoice {{invoiceNumber}} of {{remainingAmount}} " +
                "due on {{dueDate}}. Please confirm receipt: {{confirmationLink}}."
        );
    }

    // -------------------------------------------------------------------------
    // One-time cleanup of legacy AUTO system templates and rules
    // -------------------------------------------------------------------------

    private void cleanupLegacySystemData() {
        // Rules must be deleted before templates (FK constraint)
        reminderRuleRepository.findByNameAndSystemDefinedTrue(LEGACY_RULE_DAY3).ifPresent(r -> {
            followUpRepository.detachReminderRule(r.getId());
            reminderRuleRepository.delete(r);
            log.info("[Seed] Removed legacy system rule '{}'", LEGACY_RULE_DAY3);
        });
        reminderRuleRepository.findByNameAndSystemDefinedTrue(LEGACY_RULE_DAY7).ifPresent(r -> {
            followUpRepository.detachReminderRule(r.getId());
            reminderRuleRepository.delete(r);
            log.info("[Seed] Removed legacy system rule '{}'", LEGACY_RULE_DAY7);
        });
        templateRepository.findByNameAndOrganizationIsNull(LEGACY_TPL_DAY3).ifPresent(t -> {
            followUpRepository.detachTemplate(t.getId());
            templateRepository.delete(t);
            log.info("[Seed] Removed legacy system template '{}'", LEGACY_TPL_DAY3);
        });
        templateRepository.findByNameAndOrganizationIsNull(LEGACY_TPL_DAY7).ifPresent(t -> {
            followUpRepository.detachTemplate(t.getId());
            templateRepository.delete(t);
            log.info("[Seed] Removed legacy system template '{}'", LEGACY_TPL_DAY7);
        });
    }

    // -------------------------------------------------------------------------
    // Template seeding helpers
    // -------------------------------------------------------------------------

/**
     * Inserts a MANUAL system template only if no system template with this name exists yet.
     * Existing user edits are never overwritten; only snake_case placeholders are migrated.
     */
    private Template ensureTemplate(String name, TemplateChannel channel, String subject, String body) {
        return templateRepository.findByNameAndOrganizationIsNull(name).map(existing -> {
            // Always sync body and subject — system templates are platform-owned, not user-editable content
            boolean dirty = false;
            if (!body.equals(existing.getBody())) {
                existing.setBody(body);
                dirty = true;
            }
            if (subject == null ? existing.getSubject() != null : !subject.equals(existing.getSubject())) {
                existing.setSubject(subject);
                dirty = true;
            }
            if (existing.getMode() != RuleMode.MANUAL) {
                existing.setMode(RuleMode.MANUAL);
                dirty = true;
            }
            if (dirty) {
                templateRepository.save(existing);
                log.info("[Seed] Synced system template '{}' (id={})", name, existing.getId());
            }
            return existing;
        }).orElseGet(() -> {
            Template t = new Template();
            t.setName(name);
            t.setChannel(channel);
            t.setSubject(subject);
            t.setBody(body);
            t.setTone(TemplateTone.POLITE);
            t.setMode(RuleMode.MANUAL);
            t.setSystemDefined(true);
            // organization stays null — platform-level, visible to all orgs
            Template saved = templateRepository.save(t);
            log.info("[Seed] Created system template '{}' (id={})", name, saved.getId());
            return saved;
        });
    }
}
