package com.flowcollect.infrastructure.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.reminder.ReminderRule;
import com.flowcollect.domain.template.Template;
import com.flowcollect.infrastructure.persistence.organization.OrganizationJpaRepository;
import com.flowcollect.infrastructure.persistence.reminder.ReminderRuleJpaRepository;
import com.flowcollect.infrastructure.persistence.template.TemplateJpaRepository;

/**
 * Seeds per-org copies of the platform's default templates and reminder rules.
 *
 * <p>System rows (systemDefined=true, organization=null) act as blueprints only —
 * they are never shown to users directly. Each org gets its own mutable copies so
 * that one org's active/inactive changes never affect another org.
 *
 * <p>All seed operations are idempotent: if a name already exists for the org, that
 * entry is skipped. This means the method is safe to call on startup for all existing
 * orgs and immediately after registration for new orgs.
 */
@Component
public class OrgDefaultDataSeeder {

    private static final Logger log = LoggerFactory.getLogger(OrgDefaultDataSeeder.class);

    private final TemplateJpaRepository templateRepository;
    private final ReminderRuleJpaRepository reminderRuleRepository;
    private final OrganizationJpaRepository organizationRepository;

    public OrgDefaultDataSeeder(
            TemplateJpaRepository templateRepository,
            ReminderRuleJpaRepository reminderRuleRepository,
            OrganizationJpaRepository organizationRepository) {
        this.templateRepository = templateRepository;
        this.reminderRuleRepository = reminderRuleRepository;
        this.organizationRepository = organizationRepository;
    }

    /**
     * Seeds default templates and rules for a single newly registered org.
     * Called immediately after org creation in both email and OAuth registration flows.
     */
    @Transactional
    public void seedForOrg(Organization org) {
        List<Template> systemTemplates = templateRepository.findBySystemDefinedTrue();
        List<ReminderRule> systemRules = reminderRuleRepository.findBySystemDefinedTrue();
        doSeed(org, systemTemplates, systemRules);
    }

    /**
     * Seeds defaults for every org in the database.
     * Called once on application startup — safe to run repeatedly because each entry
     * is skipped when a matching name already exists for the org.
     */
    @Transactional
    public void seedForAllOrgs() {
        List<Template> systemTemplates = templateRepository.findBySystemDefinedTrue();
        List<ReminderRule> systemRules = reminderRuleRepository.findBySystemDefinedTrue();
        if (systemTemplates.isEmpty() && systemRules.isEmpty()) {
            return;
        }
        for (Organization org : organizationRepository.findAll()) {
            doSeed(org, systemTemplates, systemRules);
        }
    }

    // =========================================================================
    // Core seeding logic
    // =========================================================================

    private void doSeed(
            Organization org,
            List<Template> systemTemplates,
            List<ReminderRule> systemRules) {

        UUID orgId = org.getId();

        // Build map of system templateId → name (needed to remap occurrence template IDs in rules)
        Map<UUID, String> sysTemplateIdToName = systemTemplates.stream()
                .collect(Collectors.toMap(Template::getId, Template::getName));

        // Load all already-existing org templates by name — for idempotency and rule FK resolution
        Map<String, Template> orgTemplateByName = templateRepository.findByOrganizationId(orgId)
                .stream().collect(Collectors.toMap(Template::getName, t -> t));

        // --- Seed templates ---
        for (Template sys : systemTemplates) {
            if (orgTemplateByName.containsKey(sys.getName())) {
                continue; // already seeded or user created one with the same name
            }
            Template copy = new Template();
            copy.setOrganization(org);
            copy.setName(sys.getName());
            copy.setChannel(sys.getChannel());
            copy.setSubject(sys.getSubject());
            copy.setBody(sys.getBody());
            copy.setTone(sys.getTone());
            copy.setMode(sys.getMode());
            // systemDefined stays false — the org owns this copy
            Template saved = templateRepository.save(copy);
            orgTemplateByName.put(saved.getName(), saved);
            log.info("[OrgSeed] org={} created template '{}'", orgId, saved.getName());
        }

        // Load existing org rule names for idempotency
        Set<String> existingRuleNames = reminderRuleRepository.findByOrganizationId(orgId)
                .stream().map(ReminderRule::getName).collect(Collectors.toSet());

        // --- Seed rules ---
        for (ReminderRule sys : systemRules) {
            if (existingRuleNames.contains(sys.getName())) {
                continue;
            }

            // Resolve primary template to org-owned copy
            String primaryName = sys.getTemplate() != null ? sys.getTemplate().getName() : null;
            Template primaryTemplate = primaryName != null ? orgTemplateByName.get(primaryName) : null;
            if (primaryTemplate == null) {
                log.warn("[OrgSeed] org={} skipping rule '{}' — primary template '{}' not found in org",
                        orgId, sys.getName(), primaryName);
                continue;
            }

            // Remap each occurrence template ID from system → org-owned copy
            List<UUID> remappedOccurrenceIds = new ArrayList<>();
            boolean valid = true;
            for (UUID sysId : sys.getOccurrenceTemplateIds()) {
                String name = sysTemplateIdToName.get(sysId);
                Template orgTpl = name != null ? orgTemplateByName.get(name) : null;
                if (orgTpl == null) {
                    log.warn("[OrgSeed] org={} skipping rule '{}' — occurrence template not resolved",
                            orgId, sys.getName());
                    valid = false;
                    break;
                }
                remappedOccurrenceIds.add(orgTpl.getId());
            }
            if (!valid) continue;

            ReminderRule copy = new ReminderRule();
            copy.setOrganization(org);
            copy.setName(sys.getName());
            copy.setChannel(sys.getChannel());
            copy.setTriggerType(sys.getTriggerType());
            copy.setDaysOffset(sys.getDaysOffset());
            copy.setMaxOccurrences(sys.getMaxOccurrences());
            copy.setCycleIntervalDays(sys.getCycleIntervalDays());
            copy.setTemplate(primaryTemplate);
            copy.setOccurrenceTemplateIds(remappedOccurrenceIds);
            copy.setMode(sys.getMode());
            copy.setAttachPdf(sys.isAttachPdf());
            copy.setActive(sys.isActive());
            // systemDefined stays false — the org owns this copy
            reminderRuleRepository.save(copy);
            log.info("[OrgSeed] org={} created rule '{}'", orgId, sys.getName());
        }
    }
}
