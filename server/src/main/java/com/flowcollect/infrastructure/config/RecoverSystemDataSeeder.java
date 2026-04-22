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
 *   <li>Templates — MANUAL mode, org=null, systemDefined=true. Visible to all orgs, not deletable.</li>
 *   <li>Rules — AUTO mode, org=null, systemDefined=true, active=true. Shown on the Recover page,
 *       not deletable. Controlled by the org-level AUTO ON/OFF master switch.</li>
 *   <li>Idempotent — all content is synced on every restart, so copy or structure changes
 *       deploy automatically without a migration.</li>
 * </ul>
 *
 * <h2>Recovery sequence (6 touchpoints)</h2>
 * <pre>
 *   Day  -3   Friendly heads-up       Email
 *   Day   0   Due today               Email
 *   Day  +7   Gentle nudge            Email (cycle 1)
 *   Day +14   Direct, factual         Email (cycle 2)
 *   Day +21   Firm, open dialogue     Email (cycle 3)
 *   Day +28   Final notice            Email (cycle 4)
 * </pre>
 *
 * <h2>Cycling</h2>
 * One email rule covers all four post-due touchpoints with escalating copy:
 * <ul>
 *   <li>"After Due Date" — fires at +7, +14, +21, +28 (4 occurrences, 7-day cycle).</li>
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
    private static final String TPL_R_EMAIL_MONTH   = "Email - 3-4 Weeks Overdue";
    private static final String TPL_R_EMAIL_6WEEKS  = "Email - 5-6 Weeks Overdue";
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
    private static final String RULE_R_EMAIL_AFTER_DUE = "After Due Date";
    private static final String RULE_R_SMS_PRE_DUE     = "SMS - 7 Days Before Due";
    private static final String RULE_R_SMS_AFTER_DUE   = "SMS - After Due Date";
    private static final String RULE_R_WA_AFTER_DUE    = "WhatsApp - After Due Date";

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

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

    // =========================================================================
    // Entry point
    // =========================================================================

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        renameSystemRules();   // must run before cleanup/seed so idempotency keys align
        cleanupLegacySystemData();
        seedDefaultTemplates();
        seedRecoveryTemplatesAndRules();
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
                "Invoice {{invoiceNumber}} from {{organizationName}} - due in 3 days",
                "Hi {{customerName}},\n\n" +
                "Invoice {{invoiceNumber}} for {{remainingAmount}} is due on {{dueDate}}. " +
                "You can review the details or confirm once settled at the link below.\n\n" +
                "{{confirmationLink}}\n\n" +
                "{{organizationName}}",
                RuleMode.AUTO
        );

        Template tEmailDueDay = ensureTemplate(
                TPL_R_EMAIL_DUE_DAY, TemplateChannel.EMAIL, TemplateTone.POLITE,
                "Invoice {{invoiceNumber}} from {{organizationName}} - due today",
                "Hi {{customerName}},\n\n" +
                "Invoice {{invoiceNumber}} for {{remainingAmount}} from {{organizationName}} is due today, {{dueDate}}.\n\n" +
                "{{confirmationLink}}\n\n" +
                "{{organizationName}}",
                RuleMode.AUTO
        );

        Template tEmailWeek = ensureTemplate(
                TPL_R_EMAIL_WEEK, TemplateChannel.EMAIL, TemplateTone.NEUTRAL,
                "Invoice {{invoiceNumber}} from {{organizationName}} - 7 days overdue",
                "Hi {{customerName}},\n\n" +
                "Invoice {{invoiceNumber}} for {{remainingAmount}} was due on {{dueDate}} and has not been settled.\n\n" +
                "If you have already paid, please confirm using the link below. " +
                "Payment details are also available there.\n\n" +
                "{{confirmationLink}}\n\n" +
                "{{organizationName}}",
                RuleMode.AUTO
        );

        Template tEmail2Weeks = ensureTemplate(
                TPL_R_EMAIL_2WEEKS, TemplateChannel.EMAIL, TemplateTone.NEUTRAL,
                "Invoice {{invoiceNumber}} from {{organizationName}} - 14 days overdue",
                "Hi {{customerName}},\n\n" +
                "Invoice {{invoiceNumber}} for {{remainingAmount}} is 14 days past its due date of {{dueDate}} " +
                "and remains unpaid.\n\n" +
                "Please arrange settlement when you can. If there is anything to clarify, " +
                "reach us at {{organizationEmail}}.\n\n" +
                "{{confirmationLink}}\n\n" +
                "{{organizationName}}",
                RuleMode.AUTO
        );

        Template tEmailMonth = ensureTemplate(
                TPL_R_EMAIL_MONTH, TemplateChannel.EMAIL, TemplateTone.FIRM,
                "Invoice {{invoiceNumber}} from {{organizationName}} - still outstanding",
                "Hi {{customerName}},\n\n" +
                "Invoice {{invoiceNumber}} for {{remainingAmount}} is still outstanding. It was due on {{dueDate}}.\n\n" +
                "If there is a reason for the delay, please write to us at {{organizationEmail}}. " +
                "We are happy to help. Otherwise, we would appreciate settlement at your earliest.\n\n" +
                "{{confirmationLink}}\n\n" +
                "{{organizationName}}",
                RuleMode.AUTO
        );

        Template tEmail6Weeks = ensureTemplate(
                TPL_R_EMAIL_6WEEKS, TemplateChannel.EMAIL, TemplateTone.FIRM,
                "Invoice {{invoiceNumber}} from {{organizationName}} - 28 days overdue",
                "Hi {{customerName}},\n\n" +
                "Invoice {{invoiceNumber}} for {{remainingAmount}} from {{organizationName}} has been outstanding " +
                "since {{dueDate}}, and we have not heard back despite our earlier messages.\n\n" +
                "Please get in touch at {{organizationEmail}} or settle directly using the link below. " +
                "We would like to resolve this.\n\n" +
                "{{confirmationLink}}\n\n" +
                "{{organizationName}}",
                RuleMode.AUTO
        );

        ensureTemplate(
                TPL_R_EMAIL_FINAL, TemplateChannel.EMAIL, TemplateTone.FIRM,
                "Invoice {{invoiceNumber}} from {{organizationName}} - final notice",
                "Hi {{customerName}},\n\n" +
                "Invoice {{invoiceNumber}} for {{remainingAmount}} from {{organizationName}} remains unpaid " +
                "since {{dueDate}}, and we have not received a response to our messages.\n\n" +
                "Please reach us at {{organizationEmail}} or settle using the link below.\n\n" +
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
        // 3 email rules — 6 touchpoints total:
        //
        //   1. Before-due  — single fire, -3 days, friendly heads-up
        //   2. On-due      — single fire, day 0, due today reminder
        //   3. After-due   — cyclic, offset +7, every 7 days, 4 occurrences
        //                    covers +7, +14, +21, +28 with escalating tone

        ensureRule(RULE_R_EMAIL_PRE_DUE, ReminderChannel.EMAIL,
                   ReminderTriggerType.BEFORE_DUE_DATE, -3, 1, 0,
                   tEmailPreDue, List.of());

        ensureRule(RULE_R_EMAIL_DUE_DAY, ReminderChannel.EMAIL,
                   ReminderTriggerType.ON_DUE_DATE, 0, 1, 0,
                   tEmailDueDay, List.of());

        // 4 occurrences every 7 days → +7, +14, +21, +28
        // Tone escalates: neutral → neutral → firm → firm (final notice)
        ensureRule(RULE_R_EMAIL_AFTER_DUE, ReminderChannel.EMAIL,
                   ReminderTriggerType.AFTER_DUE_DATE, 7, 4, 7,
                   tEmailWeek,
                   List.of(
                       tEmailWeek.getId(),   // +7  gentle nudge
                       tEmail2Weeks.getId(), // +14 direct, factual
                       tEmailMonth.getId(),  // +21 firm, opens dialogue
                       tEmail6Weeks.getId()  // +28 final notice
                   ));

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
        renameSystemRule(LEGACY_RULE_R_EMAIL_AFTER_DUE_PREFIXED, RULE_R_EMAIL_AFTER_DUE);
    }

    private void renameSystemRule(String oldName, String newName) {
        reminderRuleRepository.findByNameAndSystemDefinedTrue(oldName).ifPresent(r -> {
            r.setName(newName);
            reminderRuleRepository.save(r);
            log.info("[Seed] Renamed system rule '{}' → '{}'", oldName, newName);
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
     * Upserts a system-scoped AUTO rule (org=null, systemDefined=true, mode=AUTO, active=true).
     * All fields are synced on every restart — system rules are platform-owned.
     *
     * <p>Rules are seeded as {@code active=true}. The org-level AUTO ON/OFF master switch
     * on the Recover page is the user-facing gate; individual rule toggles allow fine-grained
     * control without the complexity of a separate activation step on first use.
     *
     * @param occurrenceTemplateIds Ordered per-occurrence template IDs. Empty = use primary template for all.
     */
    private void ensureRule(
            String name, ReminderChannel channel,
            ReminderTriggerType triggerType, int daysOffset,
            int maxOccurrences, int cycleIntervalDays,
            Template primaryTemplate, List<UUID> occurrenceTemplateIds) {

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
            rule.setActive(true);
            rule.setAttachPdf(true);
            // organization stays null — platform-level, applies to all orgs via findActiveAutoRulesForOrg
            ReminderRule saved = reminderRuleRepository.save(rule);
            log.info("[Seed] Created system rule '{}' (id={})", name, saved.getId());
        });
    }
}
