package com.flowcollect.infrastructure.config;

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
import com.flowcollect.infrastructure.persistence.reminder.ReminderRuleJpaRepository;
import com.flowcollect.infrastructure.persistence.template.TemplateJpaRepository;

/**
 * Seeds platform-defined AUTO reminder rules and their templates on startup.
 *
 * Idempotency: checks by name (system-scoped) before inserting — safe to run on every restart.
 * User edits (toggle active, change body/subject) are preserved across restarts because we
 * only check for existence, never overwrite. Renaming a system rule is unsupported
 * (the UI should prevent it for systemDefined=true entries).
 */
@Component
public class RecoverSystemDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RecoverSystemDataSeeder.class);

    // Stable names used as idempotency keys — do not rename these constants.
    static final String TPL_DAY3_NAME  = "Day 3 Recovery Nudge";
    static final String TPL_DAY7_NAME  = "Day 7 Recovery Reminder";
    static final String RULE_DAY3_NAME = "System: 3-Day Overdue WhatsApp";
    static final String RULE_DAY7_NAME = "System: 7-Day Overdue Email";

    private final TemplateJpaRepository templateRepository;
    private final ReminderRuleJpaRepository reminderRuleRepository;

    public RecoverSystemDataSeeder(
            TemplateJpaRepository templateRepository,
            ReminderRuleJpaRepository reminderRuleRepository) {
        this.templateRepository     = templateRepository;
        this.reminderRuleRepository = reminderRuleRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Template tplDay3 = ensureTemplate(
                TPL_DAY3_NAME,
                TemplateChannel.WHATSAPP,
                null,
                "Hi {{customer_name}}, a quick reminder that invoice {{invoice_number}} for {{amount}} " +
                "was due {{days_overdue}} days ago. Pay now: {{payment_link}}. Reply STOP to opt out."
        );

        Template tplDay7 = ensureTemplate(
                TPL_DAY7_NAME,
                TemplateChannel.EMAIL,
                "Action Required: Invoice {{invoice_number}} is 7 days overdue",
                "Dear {{customer_name}},\n\n" +
                "Invoice {{invoice_number}} for {{amount}} issued on {{issue_date}} remains unpaid " +
                "and is now 7 days past its due date.\n\n" +
                "Please settle this at your earliest convenience: {{payment_link}}\n\n" +
                "If you have already paid, please disregard this message.\n\n" +
                "Regards,\n{{organization_name}}"
        );

        ensureRule(RULE_DAY3_NAME, 3, ReminderTriggerType.AFTER_DUE_DATE, ReminderChannel.WHATSAPP, tplDay3);
        ensureRule(RULE_DAY7_NAME, 7, ReminderTriggerType.AFTER_DUE_DATE, ReminderChannel.EMAIL,    tplDay7);
    }

    /**
     * Inserts the template only if no system template with this name exists yet.
     * Existing user edits (body, subject, active flag) are never overwritten.
     */
    private Template ensureTemplate(String name, TemplateChannel channel, String subject, String body) {
        return templateRepository.findByNameAndOrganizationIsNull(name).orElseGet(() -> {
            Template t = new Template();
            t.setName(name);
            t.setChannel(channel);
            t.setSubject(subject);
            t.setBody(body);
            t.setTone(TemplateTone.POLITE);
            t.setSystemDefined(true);
            // organization stays null — platform-level, shared across all orgs
            Template saved = templateRepository.save(t);
            log.info("[Seed] Created system template '{}' (id={})", name, saved.getId());
            return saved;
        });
    }

    /**
     * Inserts the rule only if no system rule with this name exists yet.
     * Checks regardless of active state so a user-deactivated rule is not re-inserted.
     */
    private void ensureRule(String name, int daysOffset, ReminderTriggerType triggerType,
                             ReminderChannel channel, Template template) {
        if (reminderRuleRepository.findByNameAndSystemDefinedTrue(name).isPresent()) {
            return;
        }

        ReminderRule rule = new ReminderRule();
        rule.setName(name);
        rule.setDaysOffset(daysOffset);
        rule.setTriggerType(triggerType);
        rule.setChannel(channel);
        rule.setTemplate(template);
        rule.setMode(RuleMode.AUTO);
        rule.setMaxOccurrences(1);
        rule.setCycleIntervalDays(0);
        rule.setSystemDefined(true);
        rule.activate();
        // organization stays null — platform-level

        ReminderRule saved = reminderRuleRepository.save(rule);
        log.info("[Seed] Created system reminder rule '{}' (id={})", name, saved.getId());
    }
}
