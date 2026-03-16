package com.flowcollect.application.reminder;

import com.flowcollect.application.template.TemplateService;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;

import com.flowcollect.api.v1.reminderrule.dto.ReminderRuleRequest;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.domain.reminder.ReminderRule;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.reminder.ReminderRuleJpaRepository;

@Service
public class ReminderRuleService {
    private final TemplateService templateService;
    private final OrganizationService organizationService;
    private final ReminderRuleJpaRepository reminderRuleRepository;

    public ReminderRuleService
    (
        OrganizationService organizationService, 
        ReminderRuleJpaRepository reminderRuleRepository, TemplateService templateService
    ) {
        this.organizationService = organizationService;
        this.reminderRuleRepository = reminderRuleRepository;
        this.templateService = templateService;
    }

    public ReminderRule createReminderRule
    (
        UUID organizationId, 
        ReminderRuleRequest reminderRuleRequest
    ) {
        if(reminderRuleRequest == null) {
            throw new ValidationException( 
                "Reminder rule request cannot be null");
        }
        if (reminderRuleRequest.getOrganizationId() != null
                && !reminderRuleRequest.getOrganizationId().equals(organizationId)) {
            throw new ValidationException("Organization ID in request must match path organization ID");
        }
        Organization organization = organizationService.getById(organizationId);

        ReminderRule reminderRule = new ReminderRule();
        reminderRule.setOrganization(organization);
        if(reminderRuleRequest.getName() == null) {
            throw new ValidationException( 
                "Reminder rule name cannot be null");
        }
        reminderRule.setName(reminderRuleRequest.getName());
        if (reminderRuleRequest.getDaysOffset() == null) {
            throw new ValidationException("daysOffset cannot be null");
        }
        reminderRule.setDaysOffset(reminderRuleRequest.getDaysOffset());
        if(reminderRuleRequest.getTriggerType() == null) {
            throw new ValidationException( 
                "Reminder rule trigger type cannot be null");
        }
        reminderRule.setTriggerType(reminderRuleRequest.getTriggerType());
        if(reminderRuleRequest.getChannel() == null) {
            throw new ValidationException( 
                "Reminder rule channel cannot be null");
        }
        reminderRule.setChannel(reminderRuleRequest.getChannel());
        if(reminderRuleRequest.getTemplateId() == null) {
            throw new ValidationException( 
                "Reminder rule template cannot be null");
            }
        reminderRule.setTemplate(templateService.getTemplateById(reminderRuleRequest.getTemplateId()
    
    ));
        if(reminderRuleRequest.isActive() == true) {
            reminderRule.setActive(true);
        }

        int maxOccurrences = reminderRuleRequest.getMaxOccurrences() != null
                ? reminderRuleRequest.getMaxOccurrences()
                : 1;
        if (maxOccurrences < 1) {
            throw new ValidationException("maxOccurrences must be at least 1");
        }

        int cycleIntervalDays = reminderRuleRequest.getCycleIntervalDays() != null
                ? reminderRuleRequest.getCycleIntervalDays()
                : 0;
        if (maxOccurrences > 1 && cycleIntervalDays < 1) {
            throw new ValidationException("cycleIntervalDays must be at least 1 when maxOccurrences > 1");
        }
        // Normalize: interval is irrelevant and misleading when there is only one occurrence.
        if (maxOccurrences == 1) {
            cycleIntervalDays = 0;
        }

        reminderRule.setMaxOccurrences(maxOccurrences);
        reminderRule.setCycleIntervalDays(cycleIntervalDays);
        reminderRule.setStartDate(reminderRuleRequest.getStartDate());

        return reminderRuleRepository.save(reminderRule);
    }

    public List<ReminderRule> getActiveReminderRules(UUID organizationId) {
        return reminderRuleRepository.findByOrganizationIdAndActiveTrue(organizationId);
    }

    public ReminderRule getReminderRule
    (
        UUID organizationId, 
        UUID reminderRuleId
    ) {
        // Validate organization
        organizationService.getById(organizationId);
        return ReminderUtil.validateReminderRuleAndOrganization(
            reminderRuleId, organizationId, reminderRuleRepository
        );
    }

    public Page<ReminderRule> getAllReminderRules
    (
        UUID organizationId, 
        String name, 
        Pageable pageable
    ) {
        // Validate organization
        organizationService.getById(organizationId);

        Specification<ReminderRule> specification = (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.equal(root.get("organization").get("id"), organizationId);
            if(name != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.like(root.get("name"), "%" + name + "%"));
            }
            return predicate;
        };

        return reminderRuleRepository.findAll(specification, pageable);
    }

    public ReminderRule updateReminderRule
    (
        UUID organizationId, 
        UUID reminderRuleId,
        ReminderRuleRequest reminderRuleRequest
    ) {
        if (reminderRuleRequest == null) {
            throw new ValidationException("Reminder rule request cannot be null");
        }
        if (reminderRuleRequest.getOrganizationId() != null
                && !reminderRuleRequest.getOrganizationId().equals(organizationId)) {
            throw new ValidationException("Organization ID in request must match path organization ID");
        }
        // Validate organization
        organizationService.getById(organizationId);
        ReminderRule reminderRule = ReminderUtil.validateReminderRuleAndOrganization(
            reminderRuleId, organizationId, reminderRuleRepository
        );

        if(reminderRuleRequest.getName() != null) {
            reminderRule.setName(reminderRuleRequest.getName());
        }
        if (reminderRuleRequest.getDaysOffset() != null) {
            reminderRule.setDaysOffset(reminderRuleRequest.getDaysOffset());
        }
        if(reminderRuleRequest.getTriggerType() != null) {
            reminderRule.setTriggerType(reminderRuleRequest.getTriggerType());
        }
        if(reminderRuleRequest.getChannel() != null) {
            reminderRule.setChannel(reminderRuleRequest.getChannel());
        }
        if(reminderRuleRequest.getTemplateId() == null) {
            throw new ValidationException( 
                "Reminder rule template cannot be null");
        }
        reminderRule.setTemplate(templateService.getTemplateById(reminderRuleRequest.getTemplateId()));
        
        if(reminderRuleRequest.isActive() == true) {
            reminderRule.activate();
        }
        if (reminderRuleRequest.getStartDate() != null) {
            reminderRule.setStartDate(reminderRuleRequest.getStartDate());
        }

        // Update cycle fields only when at least one is explicitly provided.
        Integer newMaxOccurrences = reminderRuleRequest.getMaxOccurrences();
        Integer newCycleIntervalDays = reminderRuleRequest.getCycleIntervalDays();
        if (newMaxOccurrences != null || newCycleIntervalDays != null) {
            int maxOccurrences = newMaxOccurrences != null ? newMaxOccurrences : reminderRule.getMaxOccurrences();
            int cycleIntervalDays = newCycleIntervalDays != null ? newCycleIntervalDays : reminderRule.getCycleIntervalDays();

            if (maxOccurrences < 1) {
                throw new ValidationException("maxOccurrences must be at least 1");
            }
            if (maxOccurrences > 1 && cycleIntervalDays < 1) {
                throw new ValidationException("cycleIntervalDays must be at least 1 when maxOccurrences > 1");
            }
            if (maxOccurrences == 1) {
                cycleIntervalDays = 0;
            }

            reminderRule.setMaxOccurrences(maxOccurrences);
            reminderRule.setCycleIntervalDays(cycleIntervalDays);
        }

        return reminderRuleRepository.save(reminderRule);
    }

    public void deleteReminderRule
    (
        UUID organizationId,
        UUID reminderRuleId
    ) {
        // Validate organization
        organizationService.getById(organizationId);
        ReminderRule reminderRule = ReminderUtil.validateReminderRuleAndOrganization(
            reminderRuleId, organizationId, reminderRuleRepository
        );
        reminderRuleRepository.delete(reminderRule);
    }

    public void activateReminderRule
    (
        UUID organizationId, 
        UUID reminderRuleId
    ) {
        // Validate organization
        organizationService.getById(organizationId);
        ReminderRule reminderRule = ReminderUtil.validateReminderRuleAndOrganization(
            reminderRuleId, organizationId, reminderRuleRepository
        );
        reminderRule.activate();
        reminderRuleRepository.save(reminderRule);
    }

    public void deactivateReminderRule
    (
        UUID organizationId, 
        UUID reminderRuleId
    ) {
        // Validate organization
        organizationService.getById(organizationId);
        ReminderRule reminderRule = ReminderUtil.validateReminderRuleAndOrganization(
            reminderRuleId, organizationId, reminderRuleRepository
        );
        reminderRule.deactivate();
        reminderRuleRepository.save(reminderRule);
    }
}
