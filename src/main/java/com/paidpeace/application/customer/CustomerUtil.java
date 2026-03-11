package com.paidpeace.application.customer;

import java.util.UUID;

import com.paidpeace.domain.customer.Customer;
import com.paidpeace.exception.http.NotFoundException;
import com.paidpeace.exception.http.ValidationException;
import com.paidpeace.infrastructure.persistence.customer.CustomerJpaRepository;

public class CustomerUtil {
    public static Customer getCustomerOrThrow
    (
        UUID customerId, 
        CustomerJpaRepository customerRepository
    ) {
        if (customerId == null) {
            throw new ValidationException("Customer ID must not be null");
        }
        Customer customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new NotFoundException("Customer not found with ID: " + customerId));
        if (customer.getOrganization().isDeleted()) {
            throw new NotFoundException("Organization is archived with ID: " + customer.getOrganization().getId());
        }
        return customer;
    }

    public static Customer validateCustomerWithOrganization
    (
        UUID customerId, 
        UUID organizationId, 
        CustomerJpaRepository customerRepository
    ) {
        if (customerId == null) {
            throw new ValidationException("Customer ID must not be null");
        }
        if (organizationId == null) {
            throw new ValidationException("Organization ID must not be null");
        }
        Customer customer = getCustomerOrThrow(customerId, customerRepository);
        if (!customer.getOrganization().getId().equals(organizationId)) {
            throw new ValidationException("Customer is not associated with organization with ID: " + organizationId);
        }
        return customer;
    }
}
