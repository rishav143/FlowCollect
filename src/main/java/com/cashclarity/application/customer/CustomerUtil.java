package com.cashclarity.application.customer;

import java.util.UUID;

import com.cashclarity.domain.customer.Customer;
import com.cashclarity.exception.customer.CustomerNotFoundException;
import com.cashclarity.exception.customer.InvalidCustomerFieldException;
import com.cashclarity.exception.organization.InvalidOrganizationFieldException;
import com.cashclarity.exception.organization.OrganizationNotFoundException;
import com.cashclarity.infrastructure.persistence.customer.CustomerJpaRepository;

public class CustomerUtil {
    public static Customer getCustomerOrThrow(UUID customerId, CustomerJpaRepository customerRepository) {
        if (customerId == null) {
            throw new InvalidCustomerFieldException("customerId must not be null");
        }
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("customer not found with id " + customerId));
        if (customer.getOrganization().isDeleted()) {
            throw new OrganizationNotFoundException("organization is archived with id " + customer.getOrganization().getId());
        }
        return customer;
    }

    public static Customer validateCustomerWithOrganization(UUID customerId, UUID organizationId, CustomerJpaRepository customerRepository) {
        if (customerId == null) {
            throw new InvalidCustomerFieldException("customerId must not be null");
        }
        if (organizationId == null) {
            throw new InvalidOrganizationFieldException("organizationId must not be null");
        }
        Customer customer = getCustomerOrThrow(customerId, customerRepository);
        if (customer.getOrganization().getId() != organizationId) {
            throw new InvalidCustomerFieldException("customer is not associated with organization with id " + organizationId);
        }
        return customer;
    }
}
