package com.flowcollect.infrastructure.config;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.flowcollect.domain.reminder.ReminderChannel;
import com.flowcollect.domain.reminder.ReminderRule;
import com.flowcollect.domain.reminder.ReminderTriggerType;
import com.flowcollect.domain.reminder.RuleMode;
import com.flowcollect.domain.template.Template;
import com.flowcollect.domain.template.TemplateChannel;
import com.flowcollect.domain.template.TemplateTone;
import com.flowcollect.infrastructure.persistence.invoice.FollowUpJpaRepository;
import com.flowcollect.infrastructure.persistence.reminder.ReminderRuleJpaRepository;
import com.flowcollect.infrastructure.persistence.template.TemplateJpaRepository;

/**
 * Seeds platform-defined system templates and AUTO recovery rules on startup.
 *
 * <h2>Design principles</h2>
 * <ul>
 *   <li>Templates — MANUAL mode, org=null, systemDefined=true. Blueprint only — each org gets its own copy.</li>
 *   <li>Rules — AUTO mode, org=null, systemDefined=true. Blueprint only — each org gets its own copy
 *       seeded by {@link OrgDefaultDataSeeder}. Never shown to users directly.</li>
 *   <li>Idempotent — all content is synced on every restart, so copy or structure changes
 *       deploy automatically without a migration.</li>
 * </ul>
 *
 * <h2>Recovery sequence (6 touchpoints)</h2>
 * <pre>
 *   Day  -3   Friendly heads-up       Email
 *   Day   0   Due today               Email
 *   Day  +7   Gentle nudge            Email
 *   Day +14   Direct, factual         Email
 *   Day +21   Firm, open dialogue     Email
 *   Day +28   Final notice            Email
 * </pre>
 *
 * <h2>Post-due rules</h2>
 * Four individual email rules cover the post-due touchpoints with escalating tone:
 * <ul>
 *   <li>"7 Days After Due"  — fires once at +7</li>
 *   <li>"14 Days After Due" — fires once at +14</li>
 *   <li>"21 Days After Due" — fires once at +21</li>
 *   <li>"28 Days After Due" — fires once at +28</li>
 * </ul>
 */
@Component
public class RecoverSystemDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RecoverSystemDataSeeder.class);

    // -------------------------------------------------------------------------
    // Legacy names — kept only for cleanup; never re-use
    // -------------------------------------------------------------------------

    private static final String LEGACY_TPL_DAY3             = "Day 3 Recovery Nudge";
    private static final String LEGACY_TPL_DAY7             = "Day 7 Recovery Reminder";
    private static final String LEGACY_RULE_DAY3            = "System: 3-Day Overdue WhatsApp";
    private static final String LEGACY_RULE_DAY7            = "System: 7-Day Overdue Email";
    // Intermediate 10-rule structure
    private static final String LEGACY_RULE_EMAIL_EARLY     = "Recovery: Early Overdue Email";
    private static final String LEGACY_RULE_EMAIL_MONTH     = "Recovery: Month Overdue Email";
    private static final String LEGACY_RULE_EMAIL_FINAL     = "Recovery: Final Notice Email";
    private static final String LEGACY_RULE_SMS_2WEEKS      = "Recovery: 2 Weeks Overdue SMS";
    private static final String LEGACY_RULE_SMS_FINAL       = "Recovery: Final Push SMS";
    private static final String LEGACY_RULE_WA_WEEK         = "Recovery: Week Overdue WhatsApp";
    private static final String LEGACY_RULE_WA_MONTH        = "Recovery: Month Overdue WhatsApp";
    // Pre-rename rule names (replaced with user-friendly names)
    private static final String LEGACY_RULE_EMAIL_PRE_DUE   = "Recovery: Pre-Due Email";
    private static final String LEGACY_RULE_EMAIL_DUE_DAY   = "Recovery: Due Day Email";
    private static final String LEGACY_RULE_EMAIL_AFTER_DUE = "Recovery: After Due Email";
    private static final String LEGACY_RULE_SMS_PRE_DUE     = "Recovery: Pre-Due SMS";
    private static final String LEGACY_RULE_SMS_AFTER_DUE   = "Recovery: After Due SMS";
    private static final String LEGACY_RULE_WA_AFTER_DUE    = "Recovery: After Due WhatsApp";
    // Pre-rename template names
    private static final String LEGACY_TPL_EMAIL_PRE_DUE    = "Recovery: Pre-Due Email";
    private static final String LEGACY_TPL_EMAIL_DUE_DAY    = "Recovery: Due Day Email";
    private static final String LEGACY_TPL_EMAIL_WEEK       = "Recovery: Week Overdue Email";
    private static final String LEGACY_TPL_EMAIL_2WEEKS     = "Recovery: 2 Weeks Overdue Email";
    private static final String LEGACY_TPL_EMAIL_MONTH      = "Recovery: Month Overdue Email";
    private static final String LEGACY_TPL_EMAIL_6WEEKS     = "Recovery: 6 Weeks Overdue Email";
    private static final String LEGACY_TPL_EMAIL_FINAL      = "Recovery: Final Notice Email";
    private static final String LEGACY_TPL_SMS_PRE_DUE      = "Recovery: Pre-Due SMS";
    private static final String LEGACY_TPL_SMS_2WEEKS       = "Recovery: 2 Weeks Overdue SMS";
    private static final String LEGACY_TPL_SMS_FINAL        = "Recovery: Final Push SMS";
    private static final String LEGACY_TPL_WA_WEEK          = "Recovery: Week Overdue WhatsApp";
    private static final String LEGACY_TPL_WA_MONTH         = "Recovery: Month Overdue WhatsApp";
    // "Recovery " prefix names (replaced with prefix-less names)
    private static final String LEGACY_TPL_PFX_EMAIL_PRE_DUE = "Recovery Email - Before Due Date";
    private static final String LEGACY_TPL_PFX_EMAIL_DUE_DAY = "Recovery Email - Due Date";
    private static final String LEGACY_TPL_PFX_EMAIL_WEEK    = "Recovery Email - 1 Week Overdue";
    private static final String LEGACY_TPL_PFX_EMAIL_2WEEKS  = "Recovery Email - 2 Weeks Overdue";
    private static final String LEGACY_TPL_PFX_EMAIL_MONTH   = "Recovery Email - 3-4 Weeks Overdue";
    private static final String LEGACY_TPL_PFX_EMAIL_6WEEKS  = "Recovery Email - 5-6 Weeks Overdue";
    private static final String LEGACY_TPL_PFX_EMAIL_FINAL   = "Recovery Email - Final Notice";
    private static final String LEGACY_TPL_PFX_SMS_PRE_DUE   = "Recovery SMS - Before Due Date";
    private static final String LEGACY_TPL_PFX_SMS_2WEEKS    = "Recovery SMS - 2 Weeks Overdue";
    private static final String LEGACY_TPL_PFX_SMS_FINAL     = "Recovery SMS - Final Notice";
    private static final String LEGACY_TPL_PFX_WA_WEEK       = "Recovery WhatsApp - 1 Week Overdue";
    private static final String LEGACY_TPL_PFX_WA_MONTH      = "Recovery WhatsApp - 4 Weeks Overdue";
    // Em-dash template names (replaced with hyphen names)
    private static final String LEGACY_TPL_EM_EMAIL_PRE_DUE = "Recovery Email \u2014 Before Due Date";
    private static final String LEGACY_TPL_EM_EMAIL_DUE_DAY = "Recovery Email \u2014 Due Date";
    private static final String LEGACY_TPL_EM_EMAIL_WEEK    = "Recovery Email \u2014 1 Week Overdue";
    private static final String LEGACY_TPL_EM_EMAIL_2WEEKS  = "Recovery Email \u2014 2 Weeks Overdue";
    private static final String LEGACY_TPL_EM_EMAIL_MONTH   = "Recovery Email \u2014 3-4 Weeks Overdue";
    private static final String LEGACY_TPL_EM_EMAIL_6WEEKS  = "Recovery Email \u2014 5-6 Weeks Overdue";
    private static final String LEGACY_TPL_EM_EMAIL_FINAL   = "Recovery Email \u2014 Final Notice";
    private static final String LEGACY_TPL_EM_SMS_PRE_DUE   = "Recovery SMS \u2014 Before Due Date";
    private static final String LEGACY_TPL_EM_SMS_2WEEKS    = "Recovery SMS \u2014 2 Weeks Overdue";
    private static final String LEGACY_TPL_EM_SMS_FINAL     = "Recovery SMS \u2014 Final Notice";
    private static final String LEGACY_TPL_EM_WA_WEEK       = "Recovery WhatsApp \u2014 1 Week Overdue";
    private static final String LEGACY_TPL_EM_WA_MONTH      = "Recovery WhatsApp \u2014 4 Weeks Overdue";
    // Em-dash rule names (replaced with hyphen names)
    private static final String LEGACY_RULE_EM_EMAIL_PRE_DUE   = "Email \u2014 7 Days Before Due";
    private static final String LEGACY_RULE_EM_EMAIL_DUE_DAY   = "Email \u2014 On Due Date";
    private static final String LEGACY_RULE_EM_EMAIL_AFTER_DUE = "Email \u2014 After Due Date";
    private static final String LEGACY_RULE_EM_SMS_PRE_DUE     = "SMS \u2014 7 Days Before Due";
    private static final String LEGACY_RULE_EM_SMS_AFTER_DUE   = "SMS \u2014 After Due Date";
    private static final String LEGACY_RULE_EM_WA_AFTER_DUE    = "WhatsApp \u2014 After Due Date";
    // 7-day pre-due rule (replaced with 3-day version)
    private static final String LEGACY_RULE_R_EMAIL_PRE_DUE_7D = "Email - 7 Days Before Due";
    // "Email -" prefix rules (replaced with timing-only names to avoid confusion with template names)
    private static final String LEGACY_RULE_R_EMAIL_PRE_DUE_PREFIXED   = "Email - 3 Days Before Due";
    private static final String LEGACY_RULE_R_EMAIL_DUE_DAY_PREFIXED   = "Email - On Due Date";
    private static final String LEGACY_RULE_R_EMAIL_AFTER_DUE_PREFIXED = "Email - After Due Date";
    // Cyclic "After Due Date" rule (split into 4 separate rules)
    private static final String LEGACY_RULE_R_EMAIL_AFTER_DUE_CYCLIC   = "After Due Date";
    // Templates renamed in-place via renameSystemTemplates() — not in the delete cleanup list
    // (deleting a template that a live rule still references would violate the NOT NULL FK constraint)
    private static final String LEGACY_TPL_R_EMAIL_6WEEKS = "Email - 5-6 Weeks Overdue"; // → "Email - Final Notice"
    private static final String LEGACY_TPL_R_EMAIL_MONTH  = "Email - 3-4 Weeks Overdue"; // → "Email - 3 Weeks Overdue"

    // -------------------------------------------------------------------------
    // Default MANUAL templates (used in the manual reminders template picker)
    // -------------------------------------------------------------------------

    static final String TPL_DEFAULT_EMAIL    = "Default Email";
    static final String TPL_DEFAULT_SMS      = "Default SMS";
    static final String TPL_DEFAULT_WHATSAPP = "Default WhatsApp";

    // -------------------------------------------------------------------------
    // Recovery template names — idempotency keys (email)
    // -------------------------------------------------------------------------

    private static final String TPL_R_EMAIL_PRE_DUE = "Email - Before Due Date";
    private static final String TPL_R_EMAIL_DUE_DAY = "Email - Due Date";
    private static final String TPL_R_EMAIL_WEEK    = "Email - 1 Week Overdue";
    private static final String TPL_R_EMAIL_2WEEKS  = "Email - 2 Weeks Overdue";
    private static final String TPL_R_EMAIL_MONTH   = "Email - 3 Weeks Overdue";
    private static final String TPL_R_EMAIL_FINAL   = "Email - Final Notice";

    // Recovery template names — idempotency keys (SMS)
    private static final String TPL_R_SMS_PRE_DUE = "SMS - Before Due Date";
    private static final String TPL_R_SMS_2WEEKS   = "SMS - 2 Weeks Overdue";
    private static final String TPL_R_SMS_FINAL    = "SMS - Final Notice";

    // Recovery template names — idempotency keys (WhatsApp)
    private static final String TPL_R_WA_WEEK  = "WhatsApp - 1 Week Overdue";
    private static final String TPL_R_WA_MONTH = "WhatsApp - 4 Weeks Overdue";

    // -------------------------------------------------------------------------
    // Recovery rule names — idempotency keys
    // -------------------------------------------------------------------------

    private static final String RULE_R_EMAIL_PRE_DUE   = "3 Days Before Due";
    private static final String RULE_R_EMAIL_DUE_DAY   = "On Due Date";
    private static final String RULE_R_EMAIL_7D        = "7 Days After Due";
    private static final String RULE_R_EMAIL_14D       = "14 Days After Due";
    private static final String RULE_R_EMAIL_21D       = "21 Days After Due";
    private static final String RULE_R_EMAIL_28D       = "28 Days After Due";
    private static final String RULE_R_SMS_PRE_DUE     = "SMS - 7 Days Before Due";
    private static final String RULE_R_SMS_AFTER_DUE   = "SMS - After Due Date";
    private static final String RULE_R_WA_AFTER_DUE    = "WhatsApp - After Due Date";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final TemplateJpaRepository     templateRepository;
    private final ReminderRuleJpaRepository reminderRuleRepository;
    private final FollowUpJpaRepository     followUpRepository;
    private final OrgDefaultDataSeeder      orgDefaultDataSeeder;

    public RecoverSystemDataSeeder(
            TemplateJpaRepository templateRepository,
            ReminderRuleJpaRepository reminderRuleRepository,
            FollowUpJpaRepository followUpRepository,
            OrgDefaultDataSeeder orgDefaultDataSeeder) {
        this.templateRepository     = templateRepository;
        this.reminderRuleRepository = reminderRuleRepository;
        this.followUpRepository     = followUpRepository;
        this.orgDefaultDataSeeder   = orgDefaultDataSeeder;
    }

    // =========================================================================
    // Entry point
    // =========================================================================

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        renameSystemRules();      // must run before cleanup/seed so idempotency keys align
        renameSystemTemplates();  // rename in-place to avoid nulling live FK references
        cleanupLegacySystemData();
        seedDefaultTemplates();
        seedRecoveryTemplatesAndRules();
        // Seed org-owned copies for every existing org (idempotent — skips names that already exist)
        orgDefaultDataSeeder.seedForAllOrgs();
    }

    // =========================================================================
    // Default MANUAL templates
    // =========================================================================

    private void seedDefaultTemplates() {

        ensureTemplate(
                TPL_DEFAULT_EMAIL, TemplateChannel.EMAIL, TemplateTone.POLITE,
                "Invoice {{invoiceNumber}} from {{organizationName}}",
                "Hi {{customerName}},\n\n" +
                "This is a reminder for invoice {{invoiceNumber}} from {{organizationName}} " +
                "for {{remainingAmount}}, due on {{dueDate}}.\n\n" +
                "If you have already made a payment, please confirm using the link below. " +
                "Payment details are available there as well.\n\n" +
                "{{confirmationLink}}\n\n" +
                "For any questions, write to us at {{organizationEmail}}.\n\n" +
                "{{organizationName}}",
                RuleMode.MANUAL
        );

        ensureTemplate(
                TPL_DEFAULT_SMS, TemplateChannel.SMS, TemplateTone.POLITE,
                null,
                "Hi {{customerName}}, this is a reminder for invoice {{invoiceNumber}} from {{organizationName}} " +
                "for {{remainingAmount}}, due on {{dueDate}}. To pay or confirm a payment already made, visit: {{confirmationLink}}",
                RuleMode.MANUAL
        );

        ensureTemplate(
                TPL_DEFAULT_WHATSAPP, TemplateChannel.WHATSAPP, TemplateTone.POLITE,
                null,
                "Hi {{customerName}},\n\n" +
                "A quick note regarding invoice {{invoiceNumber}} for *{{remainingAmount}}*, due on *{{dueDate}}*.\n\n" +
                "The link below includes payment details if you have not yet settled this, and a way to confirm " +
                "once your payment is done — so we can update our records promptly:\n" +
                "{{confirmationLink}}\n\n" +
                "For any questions, please reach out to us at {{organizationEmail}}.\n\n" +
                "Thank you,\n{{organizationName}}",
                RuleMode.MANUAL
        );
    }

    // =========================================================================
    // Recovery templates and rules
    // =========================================================================

    private void seedRecoveryTemplatesAndRules() {

        // ── Email templates ───────────────────────────────────────────────────

        Template tEmailPreDue = ensureTemplate(
                TPL_R_EMAIL_PRE_DUE, TemplateChannel.EMAIL, TemplateTone.POLITE,
                "Invoice {{invoiceNumber}} from {{organizationName}}",
                "Hi {{customerName}},\n\n" +
                "A quick note that invoice {{invoiceNumber}} for {{remainingAmount}} from {{organizationName}} " +
                "is due on {{dueDate}}.\n\n" +
                "You can review the details or confirm once payment is made at the link below:\n" +
                "{{confirmationLink}}\n\n" +
                "{{organizationName}}",
                RuleMode.AUTO
        );

        Template tEmailDueDay = ensureTemplate(
                TPL_R_EMAIL_DUE_DAY, TemplateChannel.EMAIL, TemplateTone.POLITE,
                "Invoice {{invoiceNumber}} from {{organizationName}}",
                "Hi {{customerName}},\n\n" +
                "Invoice {{invoiceNumber}} for {{remainingAmount}} from {{organizationName}} is due today.\n\n" +
                "If you have already made payment, please use the link below to confirm it so we can update our records. " +
                "Payment details are also available there:\n" +
                "{{confirmationLink}}\n\n" +
                "{{organizationName}}",
                RuleMode.AUTO
        );

        Template tEmailWeek = ensureTemplate(
                TPL_R_EMAIL_WEEK, TemplateChannel.EMAIL, TemplateTone.NEUTRAL,
                "Payment reminder: Invoice {{invoiceNumber}} from {{organizationName}}",
                "Hi {{customerName}},\n\n" +
                "This is a reminder that invoice {{invoiceNumber}} for {{remainingAmount}} from {{organizationName}} " +
                "was due on {{dueDate}}.\n\n" +
                "If you have already sent the payment, please confirm it at the link below so we can update our records. " +
                "Payment details are also available there:\n" +
                "{{confirmationLink}}\n\n" +
                "{{organizationName}}",
                RuleMode.AUTO
        );

        Template tEmail2Weeks = ensureTemplate(
                TPL_R_EMAIL_2WEEKS, TemplateChannel.EMAIL, TemplateTone.NEUTRAL,
                "Payment reminder: Invoice {{invoiceNumber}} from {{organizationName}}",
                "Hi {{customerName}},\n\n" +
                "Invoice {{invoiceNumber}} for {{remainingAmount}} from {{organizationName}} was due on {{dueDate}} " +
                "and we have not yet received payment.\n\n" +
                "If you have already paid, please confirm using the link below. " +
                "For any questions, write to us at {{organizationEmail}}.\n\n" +
                "{{confirmationLink}}\n\n" +
                "{{organizationName}}",
                RuleMode.AUTO
        );

        Template tEmailMonth = ensureTemplate(
                TPL_R_EMAIL_MONTH, TemplateChannel.EMAIL, TemplateTone.FIRM,
                "Payment reminder: Invoice {{invoiceNumber}} from {{organizationName}}",
                "Hi {{customerName}},\n\n" +
                "Invoice {{invoiceNumber}} for {{remainingAmount}} from {{organizationName}} was due on {{dueDate}}. " +
                "We have not yet received payment for this invoice.\n\n" +
                "If you have already paid, please confirm using the link below so we can close this out. " +
                "If you have any questions or need to discuss this, please write to us at {{organizationEmail}}.\n\n" +
                "{{confirmationLink}}\n\n" +
                "{{organizationName}}",
                RuleMode.AUTO
        );

        Template tEmailFinal = ensureTemplate(
                TPL_R_EMAIL_FINAL, TemplateChannel.EMAIL, TemplateTone.FIRM,
                "Payment reminder: Invoice {{invoiceNumber}} from {{organizationName}}",
                "Hi {{customerName}},\n\n" +
                "Invoice {{invoiceNumber}} for {{remainingAmount}} from {{organizationName}} has been due since {{dueDate}}. " +
                "We have not yet received payment or a response to our earlier notes.\n\n" +
                "Please get in touch at {{organizationEmail}} or use the link below to settle or confirm your payment:\n" +
                "{{confirmationLink}}\n\n" +
                "{{organizationName}}",
                RuleMode.AUTO
        );

        // ── SMS templates — disabled for now, kept for future re-activation ────

        ensureTemplate(
                TPL_R_SMS_PRE_DUE, TemplateChannel.SMS, TemplateTone.POLITE,
                null,
                "Hi {{customerName}}, invoice {{invoiceNumber}} for {{remainingAmount}} from " +
                "{{organizationName}} is due on {{dueDate}}: {{confirmationLink}}",
                RuleMode.AUTO
        );

        ensureTemplate(
                TPL_R_SMS_2WEEKS, TemplateChannel.SMS, TemplateTone.NEUTRAL,
                null,
                "Hi {{customerName}}, invoice {{invoiceNumber}} for {{remainingAmount}} from " +
                "{{organizationName}} is 14 days past its due date of {{dueDate}}. " +
                "View or settle: {{confirmationLink}}",
                RuleMode.AUTO
        );

        ensureTemplate(
                TPL_R_SMS_FINAL, TemplateChannel.SMS, TemplateTone.FIRM,
                null,
                "Hi {{customerName}}, invoice {{invoiceNumber}} for {{remainingAmount}} from " +
                "{{organizationName}} is 42 days overdue. Please arrange payment: {{confirmationLink}}",
                RuleMode.AUTO
        );

        // ── WhatsApp templates — disabled for now, kept for future re-activation

        ensureTemplate(
                TPL_R_WA_WEEK, TemplateChannel.WHATSAPP, TemplateTone.NEUTRAL,
                null,
                "Hi {{customerName}},\n\n" +
                "Invoice *{{invoiceNumber}}* for *{{remainingAmount}}* from {{organizationName}} " +
                "was due on {{dueDate}} and has not been paid.\n\n" +
                "If you have already made payment, please confirm using the link below. " +
                "Payment details are also available there.\n" +
                "{{confirmationLink}}\n\n" +
                "{{organizationName}}",
                RuleMode.AUTO
        );

        ensureTemplate(
                TPL_R_WA_MONTH, TemplateChannel.WHATSAPP, TemplateTone.FIRM,
                null,
                "Hi {{customerName}},\n\n" +
                "Invoice *{{invoiceNumber}}* for *{{remainingAmount}}* from {{organizationName}} " +
                "has been unpaid since {{dueDate}}.\n\n" +
                "If there is something to discuss, please write to us at {{organizationEmail}}. " +
                "To settle or confirm payment:\n" +
                "{{confirmationLink}}\n\n" +
                "{{organizationName}}",
                RuleMode.AUTO
        );

        // ── Rules ─────────────────────────────────────────────────────────────
        //
        // 6 email rules — 6 touchpoints total:
        //
        //   1. Before-due       — single fire, -3 days, friendly heads-up
        //   2. On-due           — single fire, day 0, due today reminder
        //   3. 7 Days After Due  — single fire, +7,  gentle nudge
        //   4. 14 Days After Due — single fire, +14, direct, factual
        //   5. 21 Days After Due — single fire, +21, firm, opens dialogue
        //   6. 28 Days After Due — single fire, +28, final notice

        ensureRule(RULE_R_EMAIL_PRE_DUE, ReminderChannel.EMAIL,
                   ReminderTriggerType.BEFORE_DUE_DATE, -3, 1, 0,
                   tEmailPreDue, List.of(), true);

        ensureRule(RULE_R_EMAIL_DUE_DAY, ReminderChannel.EMAIL,
                   ReminderTriggerType.ON_DUE_DATE, 0, 1, 0,
                   tEmailDueDay, List.of(), true);

        // 4 individual after-due rules — each fires once at its offset
        ensureRule(RULE_R_EMAIL_7D,  ReminderChannel.EMAIL,
                   ReminderTriggerType.AFTER_DUE_DATE, 7,  1, 0,
                   tEmailWeek, List.of(), true);

        ensureRule(RULE_R_EMAIL_14D, ReminderChannel.EMAIL,
                   ReminderTriggerType.AFTER_DUE_DATE, 14, 1, 0,
                   tEmail2Weeks, List.of(), true);

        // Seeded inactive by default — opt-in to avoid being aggressive with clients
        ensureRule(RULE_R_EMAIL_21D, ReminderChannel.EMAIL,
                   ReminderTriggerType.AFTER_DUE_DATE, 21, 1, 0,
                   tEmailMonth, List.of(), false);

        ensureRule(RULE_R_EMAIL_28D, ReminderChannel.EMAIL,
                   ReminderTriggerType.AFTER_DUE_DATE, 28, 1, 0,
                   tEmailFinal, List.of(), true);

        // SMS and WhatsApp rules are disabled for now — kept for future re-activation.
        // To re-enable, uncomment the blocks below and remove the rule names from cleanupLegacySystemData().
        //
        // ensureRule(RULE_R_SMS_PRE_DUE, ReminderChannel.SMS,
        //            ReminderTriggerType.BEFORE_DUE_DATE, -7, 1, 0,
        //            tSmsPreDue, List.of());
        //
        // ensureRule(RULE_R_SMS_AFTER_DUE, ReminderChannel.SMS,
        //            ReminderTriggerType.AFTER_DUE_DATE, 14, 2, 14,
        //            tSms2Weeks,
        //            List.of(tSms2Weeks.getId(), tSmsFinal.getId()));
        //
        // ensureRule(RULE_R_WA_AFTER_DUE, ReminderChannel.WHATSAPP,
        //            ReminderTriggerType.AFTER_DUE_DATE, 7, 2, 21,
        //            tWaWeek,
        //            List.of(tWaWeek.getId(), tWaMonth.getId()));
    }

    // =========================================================================
    // In-place rule renames (preserves UUID and sent-history)
    // =========================================================================

    /**
     * Renames system rules in-place so their UUID (and all associated follow-up history)
     * is preserved. Must run before cleanupLegacySystemData and seedRecoveryTemplatesAndRules
     * so the idempotency keys used by ensureRule align with the new names.
     */
    private void renameSystemRules() {
        renameSystemRule(LEGACY_RULE_R_EMAIL_PRE_DUE_PREFIXED,   RULE_R_EMAIL_PRE_DUE);
        renameSystemRule(LEGACY_RULE_R_EMAIL_DUE_DAY_PREFIXED,   RULE_R_EMAIL_DUE_DAY);
        // Rename "Email - After Due Date" → "After Due Date" so cleanup can delete it by name
        renameSystemRule(LEGACY_RULE_R_EMAIL_AFTER_DUE_PREFIXED, LEGACY_RULE_R_EMAIL_AFTER_DUE_CYCLIC);
    }

    private void renameSystemRule(String oldName, String newName) {
        reminderRuleRepository.findByNameAndSystemDefinedTrue(oldName).ifPresent(r -> {
            r.setName(newName);
            reminderRuleRepository.save(r);
            log.info("[Seed] Renamed system rule '{}' → '{}'", oldName, newName);
        });
    }

    // =========================================================================
    // In-place template renames (preserves UUID and rule FK references)
    // =========================================================================

    /**
     * Renames system templates in-place so their UUID (and all FK references from rules)
     * is preserved. Must run before cleanupLegacySystemData so that any template whose old
     * name would otherwise be deleted is already gone from the cleanup path.
     *
     * <p>Deleting a template that a live rule still references would violate the NOT NULL
     * constraint on reminder_rules.template_id. Renaming avoids that entirely.
     */
    private void renameSystemTemplates() {
        // "Email - 5-6 Weeks Overdue" → "Email - Final Notice" (+28d is the final touchpoint)
        renameSystemTemplate(LEGACY_TPL_R_EMAIL_6WEEKS, TPL_R_EMAIL_FINAL);
        // "Email - 3-4 Weeks Overdue" → "Email - 3 Weeks Overdue" (+21d = exactly 3 weeks)
        renameSystemTemplate(LEGACY_TPL_R_EMAIL_MONTH, TPL_R_EMAIL_MONTH);
    }

    private void renameSystemTemplate(String oldName, String newName) {
        templateRepository.findByNameAndOrganizationIsNull(oldName).ifPresent(t -> {
            t.setName(newName);
            templateRepository.save(t);
            log.info("[Seed] Renamed system template '{}' → '{}'", oldName, newName);
        });
    }

    // =========================================================================
    // One-time cleanup of legacy system data
    // =========================================================================

    private void cleanupLegacySystemData() {
        // Rules must be deleted before templates (FK constraint).
        // Covers both the original legacy names and the intermediate 10-rule structure.
        for (String ruleName : List.of(
                LEGACY_RULE_DAY3,
                LEGACY_RULE_DAY7,
                LEGACY_RULE_EMAIL_EARLY,
                LEGACY_RULE_EMAIL_MONTH,
                LEGACY_RULE_EMAIL_FINAL,
                LEGACY_RULE_SMS_2WEEKS,
                LEGACY_RULE_SMS_FINAL,
                LEGACY_RULE_WA_WEEK,
                LEGACY_RULE_WA_MONTH,
                LEGACY_RULE_EMAIL_PRE_DUE,
                LEGACY_RULE_EMAIL_DUE_DAY,
                LEGACY_RULE_EMAIL_AFTER_DUE,
                LEGACY_RULE_SMS_PRE_DUE,
                LEGACY_RULE_SMS_AFTER_DUE,
                LEGACY_RULE_WA_AFTER_DUE,
                LEGACY_RULE_EM_EMAIL_PRE_DUE,
                LEGACY_RULE_EM_EMAIL_DUE_DAY,
                LEGACY_RULE_EM_EMAIL_AFTER_DUE,
                LEGACY_RULE_EM_SMS_PRE_DUE,
                LEGACY_RULE_EM_SMS_AFTER_DUE,
                LEGACY_RULE_EM_WA_AFTER_DUE,
                // 7-day pre-due rule replaced by 3-day version
                LEGACY_RULE_R_EMAIL_PRE_DUE_7D,
                // Note: LEGACY_RULE_R_EMAIL_*_PREFIXED rules are handled by renameSystemRules()
                // (renamed in-place, not deleted) so they are intentionally absent here.
                // Cyclic after-due rule replaced by 4 individual rules
                LEGACY_RULE_R_EMAIL_AFTER_DUE_CYCLIC,
                // SMS/WhatsApp rules disabled — remove from DB if previously seeded
                RULE_R_SMS_PRE_DUE,
                RULE_R_SMS_AFTER_DUE,
                RULE_R_WA_AFTER_DUE
        )) {
            reminderRuleRepository.findByNameAndSystemDefinedTrue(ruleName).ifPresent(r -> {
                followUpRepository.detachReminderRule(r.getId());
                reminderRuleRepository.delete(r);
                log.info("[Seed] Removed legacy system rule '{}'", r.getName());
            });
        }
        for (String tplName : List.of(
                LEGACY_TPL_DAY3,
                LEGACY_TPL_DAY7,
                LEGACY_TPL_EMAIL_PRE_DUE,
                LEGACY_TPL_EMAIL_DUE_DAY,
                LEGACY_TPL_EMAIL_WEEK,
                LEGACY_TPL_EMAIL_2WEEKS,
                LEGACY_TPL_EMAIL_MONTH,
                LEGACY_TPL_EMAIL_6WEEKS,
                LEGACY_TPL_EMAIL_FINAL,
                LEGACY_TPL_SMS_PRE_DUE,
                LEGACY_TPL_SMS_2WEEKS,
                LEGACY_TPL_SMS_FINAL,
                LEGACY_TPL_WA_WEEK,
                LEGACY_TPL_WA_MONTH,
                LEGACY_TPL_EM_EMAIL_PRE_DUE,
                LEGACY_TPL_EM_EMAIL_DUE_DAY,
                LEGACY_TPL_EM_EMAIL_WEEK,
                LEGACY_TPL_EM_EMAIL_2WEEKS,
                LEGACY_TPL_EM_EMAIL_MONTH,
                LEGACY_TPL_EM_EMAIL_6WEEKS,
                LEGACY_TPL_EM_EMAIL_FINAL,
                LEGACY_TPL_EM_SMS_PRE_DUE,
                LEGACY_TPL_EM_SMS_2WEEKS,
                LEGACY_TPL_EM_SMS_FINAL,
                LEGACY_TPL_EM_WA_WEEK,
                LEGACY_TPL_EM_WA_MONTH,
                LEGACY_TPL_PFX_EMAIL_PRE_DUE,
                LEGACY_TPL_PFX_EMAIL_DUE_DAY,
                LEGACY_TPL_PFX_EMAIL_WEEK,
                LEGACY_TPL_PFX_EMAIL_2WEEKS,
                LEGACY_TPL_PFX_EMAIL_MONTH,
                LEGACY_TPL_PFX_EMAIL_6WEEKS,
                LEGACY_TPL_PFX_EMAIL_FINAL,
                LEGACY_TPL_PFX_SMS_PRE_DUE,
                LEGACY_TPL_PFX_SMS_2WEEKS,
                LEGACY_TPL_PFX_SMS_FINAL,
                LEGACY_TPL_PFX_WA_WEEK,
                LEGACY_TPL_PFX_WA_MONTH
        )) {
            templateRepository.findByNameAndOrganizationIsNull(tplName).ifPresent(t -> {
                followUpRepository.detachTemplate(t.getId());
                followUpRepository.detachTemplateFromSystemRules(t.getId());
                templateRepository.delete(t);
                log.info("[Seed] Removed legacy system template '{}'", t.getName());
            });
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Upserts a system-scoped template (org=null, systemDefined=true).
     * All content fields are synced on every restart — system templates are platform-owned.
     *
     * @param mode MANUAL for templates shown on the Templates page; AUTO for recovery templates
     *             used only by system AUTO rules (hidden from the Templates page).
     */
    private Template ensureTemplate(
            String name, TemplateChannel channel, TemplateTone tone,
            String subject, String body, RuleMode mode) {

        return templateRepository.findByNameAndOrganizationIsNull(name).map(existing -> {
            boolean dirty = false;
            if (!body.equals(existing.getBody()))                              { existing.setBody(body);       dirty = true; }
            if (subject == null ? existing.getSubject() != null
                                : !subject.equals(existing.getSubject()))      { existing.setSubject(subject); dirty = true; }
            if (existing.getTone() != tone)                                    { existing.setTone(tone);       dirty = true; }
            if (existing.getMode() != mode)                                    { existing.setMode(mode);       dirty = true; }
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
            t.setTone(tone);
            t.setMode(mode);
            t.setSystemDefined(true);
            // organization stays null — platform-level, visible to all orgs
            Template saved = templateRepository.save(t);
            log.info("[Seed] Created system template '{}' (id={})", name, saved.getId());
            return saved;
        });
    }

    /**
     * Upserts a system-scoped AUTO rule (org=null, systemDefined=true, mode=AUTO).
     * All fields except {@code active} are synced on every restart — system rules are platform-owned.
     *
     * <p>{@code defaultActive} controls the initial state when the rule is first created.
     * On subsequent restarts the {@code active} flag is intentionally not touched, so user
     * preferences (enable/disable individual rules) are preserved across deployments.
     *
     * @param defaultActive         Initial active state on first creation. Use {@code false} for
     *                              rules that should be opt-in (e.g. to avoid aggressive follow-ups).
     * @param occurrenceTemplateIds Ordered per-occurrence template IDs. Empty = use primary template for all.
     */
    private void ensureRule(
            String name, ReminderChannel channel,
            ReminderTriggerType triggerType, int daysOffset,
            int maxOccurrences, int cycleIntervalDays,
            Template primaryTemplate, List<UUID> occurrenceTemplateIds,
            boolean defaultActive) {

        reminderRuleRepository.findByNameAndSystemDefinedTrue(name).ifPresentOrElse(existing -> {
            boolean dirty = false;
            if (existing.getChannel() != channel)                          { existing.setChannel(channel);             dirty = true; }
            if (existing.getTriggerType() != triggerType)                  { existing.setTriggerType(triggerType);     dirty = true; }
            if (existing.getDaysOffset() != daysOffset)                    { existing.setDaysOffset(daysOffset);       dirty = true; }
            if (existing.getMaxOccurrences() != maxOccurrences)            { existing.setMaxOccurrences(maxOccurrences); dirty = true; }
            if (existing.getCycleIntervalDays() != cycleIntervalDays)      { existing.setCycleIntervalDays(cycleIntervalDays); dirty = true; }
            if (existing.getTemplate() == null || !primaryTemplate.getId().equals(existing.getTemplate().getId())) {
                existing.setTemplate(primaryTemplate);                                                                   dirty = true;
            }
            if (!occurrenceTemplateIds.equals(existing.getOccurrenceTemplateIds())) {
                existing.setOccurrenceTemplateIds(new ArrayList<>(occurrenceTemplateIds));                               dirty = true;
            }
            if (existing.getMode() != RuleMode.AUTO)                       { existing.setMode(RuleMode.AUTO);          dirty = true; }
            if (!existing.isAttachPdf())                                   { existing.setAttachPdf(true);              dirty = true; }
            // Blueprints must always reflect the coded default — user preferences live on org-owned copies
            if (existing.isActive() != defaultActive)                      { existing.setActive(defaultActive);        dirty = true; }
            if (dirty) {
                reminderRuleRepository.save(existing);
                log.info("[Seed] Synced system rule '{}' (id={})", name, existing.getId());
            }
        }, () -> {
            ReminderRule rule = new ReminderRule();
            rule.setName(name);
            rule.setChannel(channel);
            rule.setTriggerType(triggerType);
            rule.setDaysOffset(daysOffset);
            rule.setMaxOccurrences(maxOccurrences);
            rule.setCycleIntervalDays(cycleIntervalDays);
            rule.setTemplate(primaryTemplate);
            rule.setOccurrenceTemplateIds(new ArrayList<>(occurrenceTemplateIds));
            rule.setMode(RuleMode.AUTO);
            rule.setSystemDefined(true);
            rule.setActive(defaultActive);
            rule.setAttachPdf(true);
            // organization stays null — blueprint only; OrgDefaultDataSeeder copies this to each org
            ReminderRule saved = reminderRuleRepository.save(rule);
            log.info("[Seed] Created system rule '{}' (id={})", name, saved.getId());
        });
    }
}
