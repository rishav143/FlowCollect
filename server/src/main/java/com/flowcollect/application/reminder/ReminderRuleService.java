package com.flowcollect.application.reminder;

import com.flowcollect.application.template.TemplateService;
import com.flowcollect.domain.template.Template;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;

import com.flowcollect.api.v1.reminderrule.dto.ReminderRuleRequest;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.reminder.ReminderRule;
import com.flowcollect.domain.reminder.RuleMode;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.reminder.ReminderRuleJpaRepository;

@Service
public class ReminderRuleService {

    private final TemplateService templateService;
    private final OrganizationService organizationService;
    private final ReminderRuleJpaRepository reminderRuleRepository;

    public ReminderRuleService(
        OrganizationService organizationService,
        ReminderRuleJpaRepository reminderRuleRepository,
        TemplateService templateService
    ) {
        this.organizationService = organizationService;
        this.reminderRuleRepository = reminderRuleRepository;
        this.templateService = templateService;
    }

    @Transactional
    public ReminderRule createReminderRule(UUID organizationId, ReminderRuleRequest request) {
        if (request == null) {
            throw new ValidationException("Reminder rule request cannot be null");
        }
        if (request.getOrganizationId() != null && !request.getOrganizationId().equals(organizationId)) {
            throw new ValidationException("Organization ID in request must match path organization ID");
        }
        Organization organization = organizationService.getById(organizationId);

        if (request.getName() == null || request.getName().isBlank()) {
            throw new ValidationException("Reminder rule name cannot be null or blank");
        }
        String name = request.getName().trim();
        if (reminderRuleRepository.existsByNameAndOrganizationId(name, organizationId)) {
            throw new ValidationException("A reminder rule named '" + name + "' already exists for this organization");
        }
        if (request.getDaysOffset() == null) {
            throw new ValidationException("daysOffset cannot be null");
        }
        if (request.getTriggerType() == null) {
            throw new ValidationException("Trigger type cannot be null");
        }
        if (request.getChannel() == null) {
            throw new ValidationException("Channel cannot be null");
        }
        int maxOccurrences = request.getMaxOccurrences() != null ? request.getMaxOccurrences() : 1;
        if (maxOccurrences < 1) {
            throw new ValidationException("maxOccurrences must be at least 1");
        }
        int cycleIntervalDays = request.getCycleIntervalDays() != null ? request.getCycleIntervalDays() : 0;
        if (maxOccurrences > 1 && cycleIntervalDays < 1) {
            throw new ValidationException("cycleIntervalDays must be at least 1 when maxOccurrences > 1");
        }
        if (maxOccurrences == 1) {
            cycleIntervalDays = 0;
        }

        // Resolve per-occurrence templates or fall back to single templateId
        List<UUID> occurrenceTemplateIds = request.getOccurrenceTemplateIds();
        List<UUID> resolvedOccurrenceIds = new ArrayList<>();
        if (occurrenceTemplateIds != null && !occurrenceTemplateIds.isEmpty()) {
            if (occurrenceTemplateIds.size() != maxOccurrences) {
                throw new ValidationException(
                    "occurrenceTemplateIds size (" + occurrenceTemplateIds.size() +
                    ") must equal maxOccurrences (" + maxOccurrences + ")"
                );
            }
            for (int i = 0; i < occurrenceTemplateIds.size(); i++) {
                if (occurrenceTemplateIds.get(i) == null) {
                    throw new ValidationException("occurrenceTemplateIds[" + i + "] must not be null");
                }
                templateService.getTemplateById(occurrenceTemplateIds.get(i)); // validates existence
            }
            resolvedOccurrenceIds = new ArrayList<>(occurrenceTemplateIds);
        }

        // Primary template: first occurrence template if per-occurrence overrides provided, else the explicit templateId
        UUID primaryTemplateId = (resolvedOccurrenceIds.isEmpty())
            ? request.getTemplateId()
            : resolvedOccurrenceIds.get(0);
        if (primaryTemplateId == null) {
            throw new ValidationException("Template ID cannot be null");
        }

        ReminderRule rule = new ReminderRule();
        rule.setOrganization(organization);
        rule.setName(name);
        rule.setDaysOffset(request.getDaysOffset());
        rule.setTriggerType(request.getTriggerType());
        rule.setChannel(request.getChannel());
        rule.setTemplate(templateService.getTemplateById(primaryTemplateId));
        rule.setMaxOccurrences(maxOccurrences);
        rule.setCycleIntervalDays(cycleIntervalDays);
        rule.setOccurrenceTemplateIds(resolvedOccurrenceIds);
        if (request.getStartDate() != null) {
            rule.setStartDate(request.getStartDate());
        }
        rule.setMode(request.getMode() != null ? request.getMode() : RuleMode.MANUAL);
        if (Boolean.TRUE.equals(request.isActive())) {
            rule.activate();
        }

        return reminderRuleRepository.save(rule);
    }

    public List<ReminderRule> getActiveReminderRules(UUID organizationId) {
        return reminderRuleRepository.findByOrganizationIdAndActiveTrue(organizationId);
    }

    public ReminderRule getReminderRule(UUID organizationId, UUID reminderRuleId) {
        organizationService.getById(organizationId);
        return ReminderUtil.validateReminderRuleAndOrganization(reminderRuleId, organizationId, reminderRuleRepository);
    }

    public Page<ReminderRule> getAllReminderRules(UUID organizationId, String name, Pageable pageable) {
        organizationService.getById(organizationId);

        Specification<ReminderRule> spec = (root, query, cb) -> {
            // Include org-owned rules OR system-defined rules (visible to all orgs)
            Predicate orgOwned = cb.equal(root.get("organization").get("id"), organizationId);
            Predicate systemDefined = cb.isTrue(root.get("systemDefined"));
            Predicate p = cb.or(orgOwned, systemDefined);
            if (name != null && !name.isBlank()) {
                p = cb.and(p, cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%"));
            }
            return p;
        };

        return reminderRuleRepository.findAll(spec, pageable);
    }

    @Transactional
    public ReminderRule updateReminderRule(UUID organizationId, UUID reminderRuleId, ReminderRuleRequest request) {
        if (request == null) {
            throw new ValidationException("Reminder rule request cannot be null");
        }
        if (request.getOrganizationId() != null && !request.getOrganizationId().equals(organizationId)) {
            throw new ValidationException("Organization ID in request must match path organization ID");
        }
        organizationService.getById(organizationId);
        ReminderRule rule = ReminderUtil.validateReminderRuleAndOrganization(
            reminderRuleId, organizationId, reminderRuleRepository
        );

        if (request.getName() != null) {
            if (request.getName().isBlank()) {
                throw new ValidationException("Name must not be blank");
            }
            String trimmed = request.getName().trim();
            if (!trimmed.equals(rule.getName()) &&
                    reminderRuleRepository.existsByNameAndOrganizationId(trimmed, organizationId)) {
                throw new ValidationException("A reminder rule named '" + trimmed + "' already exists for this organization");
            }
            rule.setName(trimmed);
        }
        if (request.getDaysOffset() != null) {
            rule.setDaysOffset(request.getDaysOffset());
        }
        if (request.getTriggerType() != null) {
            rule.setTriggerType(request.getTriggerType());
        }
        if (request.getChannel() != null) {
            rule.setChannel(request.getChannel());
        }
        if (request.getTemplateId() != null) {
            rule.setTemplate(templateService.getTemplateById(request.getTemplateId()));
        }

        // Per-occurrence template overrides: only applied when field is explicitly provided
        List<UUID> occurrenceTemplateIds = request.getOccurrenceTemplateIds();
        if (occurrenceTemplateIds != null) {
            if (occurrenceTemplateIds.isEmpty()) {
                // Explicitly cleared — remove overrides
                rule.setOccurrenceTemplateIds(new ArrayList<>());
            } else {
                // Resolve final maxOccurrences for size validation (may have been updated above)
                int effectiveMax = request.getMaxOccurrences() != null
                    ? request.getMaxOccurrences()
                    : rule.getMaxOccurrences();
                if (occurrenceTemplateIds.size() != effectiveMax) {
                    throw new ValidationException(
                        "occurrenceTemplateIds size (" + occurrenceTemplateIds.size() +
                        ") must equal maxOccurrences (" + effectiveMax + ")"
                    );
                }
                for (int i = 0; i < occurrenceTemplateIds.size(); i++) {
                    if (occurrenceTemplateIds.get(i) == null) {
                        throw new ValidationException("occurrenceTemplateIds[" + i + "] must not be null");
                    }
                    templateService.getTemplateById(occurrenceTemplateIds.get(i));
                }
                rule.setOccurrenceTemplateIds(new ArrayList<>(occurrenceTemplateIds));
                // Keep primary template in sync with first occurrence
                rule.setTemplate(templateService.getTemplateById(occurrenceTemplateIds.get(0)));
            }
        }

        if (request.getMode() != null) {
            rule.setMode(request.getMode());
        }
        if (request.isActive() != null) {
            if (Boolean.TRUE.equals(request.isActive())) {
                rule.activate();
            } else {
                rule.deactivate();
            }
        }
        if (request.getStartDate() != null) {
            rule.setStartDate(request.getStartDate());
        }

        Integer newMax = request.getMaxOccurrences();
        Integer newInterval = request.getCycleIntervalDays();
        if (newMax != null || newInterval != null) {
            int maxOccurrences = newMax != null ? newMax : rule.getMaxOccurrences();
            int cycleIntervalDays = newInterval != null ? newInterval : rule.getCycleIntervalDays();
            if (maxOccurrences < 1) {
                throw new ValidationException("maxOccurrences must be at least 1");
            }
            if (maxOccurrences > 1 && cycleIntervalDays < 1) {
                throw new ValidationException("cycleIntervalDays must be at least 1 when maxOccurrences > 1");
            }
            if (maxOccurrences == 1) {
                cycleIntervalDays = 0;
            }
            rule.setMaxOccurrences(maxOccurrences);
            rule.setCycleIntervalDays(cycleIntervalDays);
        }

        return reminderRuleRepository.save(rule);
    }

    @Transactional
    public void deleteReminderRule(UUID organizationId, UUID reminderRuleId) {
        organizationService.getById(organizationId);
        ReminderRule rule = ReminderUtil.validateReminderRuleAndOrganization(
            reminderRuleId, organizationId, reminderRuleRepository
        );
        reminderRuleRepository.delete(rule);
    }

    @Transactional
    public ReminderRule activateReminderRule(UUID organizationId, UUID reminderRuleId) {
        organizationService.getById(organizationId);
        ReminderRule rule = ReminderUtil.validateReminderRuleAndOrganization(
            reminderRuleId, organizationId, reminderRuleRepository
        );
        rule.activate();
        return reminderRuleRepository.save(rule);
    }

    @Transactional
    public ReminderRule deactivateReminderRule(UUID organizationId, UUID reminderRuleId) {
        organizationService.getById(organizationId);
        ReminderRule rule = ReminderUtil.validateReminderRuleAndOrganization(
            reminderRuleId, organizationId, reminderRuleRepository
        );
        rule.deactivate();
        return reminderRuleRepository.save(rule);
    }

    // Returns all active system-defined AUTO rules for the recovery engine
    public List<ReminderRule> getActiveSystemRules() {
        return reminderRuleRepository.findBySystemDefinedTrueAndActiveTrueAndMode(RuleMode.AUTO);
    }

    /**
     * Returns all active AUTO rules an org should schedule: org-owned AUTO rules
     * plus platform-seeded (systemDefined) AUTO rules.
     * Used by the Recover scheduler.
     */
    public List<ReminderRule> getActiveAutoRulesForOrg(UUID orgId) {
        return reminderRuleRepository.findActiveAutoRulesForOrg(orgId);
    }
}
