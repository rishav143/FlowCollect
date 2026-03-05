package com.paidpeace.application.reminder;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import com.paidpeace.api.v1.reminderrule.dto.ReminderRuleRequest;
import com.paidpeace.application.organization.OrganizationService;
import com.paidpeace.domain.organization.Organization;
import com.paidpeace.domain.reminder.ReminderRule;
import com.paidpeace.exception.code.ReminderRuleErrorCode;
import com.paidpeace.exception.http.NotFoundException;
import com.paidpeace.infrastructure.persistence.reminder.ReminderRuleJpaRepository;

public class ReminderRuleService {
    private final OrganizationService organizationService;
    private final ReminderRuleJpaRepository reminderRuleRepository;

    public ReminderRuleService
    (
        OrganizationService organizationService, 
        ReminderRuleJpaRepository reminderRuleRepository
    ) {
        this.organizationService = organizationService;
        this.reminderRuleRepository = reminderRuleRepository;
    }

    public ReminderRule createReminderRule
    (
        UUID organizationId, 
        ReminderRuleRequest reminderRuleRequest
    ) {
        if(reminderRuleRequest == null) {
            throw new NotFoundException(ReminderRuleErrorCode.REMINDER_NOT_FOUND, 
                "Reminder rule request cannot be null");
        }
        Organization organization = organizationService.getById(organizationId);

        ReminderRule reminderRule = new ReminderRule();
        reminderRule.setOrganization(organization);
        if(reminderRuleRequest.getName() == null) {
            throw new NotFoundException(ReminderRuleErrorCode.REMINDER_NOT_FOUND, 
                "Reminder rule name cannot be null");
        }
        reminderRule.setName(reminderRuleRequest.getName());
        reminderRule.setDaysOffset(reminderRuleRequest.getDaysOffset());
        if(reminderRuleRequest.getTriggerType() == null) {
            throw new NotFoundException(ReminderRuleErrorCode.REMINDER_NOT_FOUND, 
                "Reminder rule trigger type cannot be null");
        }
        reminderRule.setTriggerType(reminderRuleRequest.getTriggerType());
        if(reminderRuleRequest.getChannel() == null) {
            throw new NotFoundException(ReminderRuleErrorCode.REMINDER_NOT_FOUND, 
                "Reminder rule channel cannot be null");
        }
        reminderRule.setChannel(reminderRuleRequest.getChannel());
        if(reminderRuleRequest.getTemplate() == null) {
            throw new NotFoundException(ReminderRuleErrorCode.REMINDER_NOT_FOUND, 
                "Reminder rule template cannot be null");
            }
        reminderRule.setTemplate(reminderRuleRequest.getTemplate());
        if(reminderRuleRequest.isActive() == true) {
            reminderRule.setActive(true);
        }

        return reminderRuleRepository.save(reminderRule);
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
            Predicate predicate = criteriaBuilder.equal(root.get("organization"), organizationId);
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
        // Validate organization
        organizationService.getById(organizationId);
        ReminderRule reminderRule = ReminderUtil.validateReminderRuleAndOrganization(
            reminderRuleId, organizationId, reminderRuleRepository
        );

        if(reminderRuleRequest.getName() != null) {
            reminderRule.setName(reminderRuleRequest.getName());
        }
        if(reminderRuleRequest.getOrganizationId() != null) {
            Organization newOrganization = organizationService.getById(reminderRuleRequest.getOrganizationId());
            reminderRule.setOrganization(newOrganization);
        }
        if(reminderRuleRequest.getDaysOffset() != 0) {
            reminderRule.setDaysOffset(reminderRuleRequest.getDaysOffset());
        }
        if(reminderRuleRequest.getTriggerType() != null) {
            reminderRule.setTriggerType(reminderRuleRequest.getTriggerType());
        }
        if(reminderRuleRequest.getChannel() != null) {
            reminderRule.setChannel(reminderRuleRequest.getChannel());
        }
        if(reminderRuleRequest.getTemplate() != null) {
            reminderRule.setTemplate(reminderRuleRequest.getTemplate());
        }
        if(reminderRuleRequest.isActive() == true) {
            reminderRule.activate();
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
