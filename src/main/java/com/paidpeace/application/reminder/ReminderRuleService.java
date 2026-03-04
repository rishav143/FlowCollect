package com.paidpeace.application.reminder;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
        reminderRule.setTriggerType(reminderRuleRequest.getTriggerType());
        reminderRule.setChannel(reminderRuleRequest.getChannel());
        reminderRule.setTemplate(reminderRuleRequest.getTemplate());
        reminderRule.setActive(reminderRuleRequest.isActive());

        return reminderRuleRepository.save(reminderRule);
    }

    public ReminderRule getReminderRule(UUID organizationId, UUID reminderRuleId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getReminderRule'");
    }

    public Page<ReminderRule> getAllReminderRules(UUID organizationId, String name, Pageable pageable) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getAllReminderRules'");
    }

    public ReminderRule updateReminderRule(UUID organizationId, UUID reminderRuleId,
            ReminderRuleRequest reminderRuleRequest) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'updateReminderRule'");
    }

    public void deleteReminderRule(UUID organizationId, UUID reminderRuleId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deleteReminderRule'");
    }

    public void activateReminderRule(UUID organizationId, UUID reminderRuleId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'activateReminderRule'");
    }

    public void deactivateReminderRule(UUID organizationId, UUID reminderRuleId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'deactivateReminderRule'");
    }
}
