package com.flowcollect.application.customer;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.flowcollect.api.v1.customer.dto.CustomerRequest;
import com.flowcollect.application.organization.OrganizationService;
import com.flowcollect.domain.customer.Customer;
import com.flowcollect.domain.organization.Organization;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.customer.CustomerJpaRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class CustomerService {
    private final OrganizationService organizationService;
    private final CustomerJpaRepository customerRepository;

    public CustomerService
    (
        OrganizationService organizationService, 
        CustomerJpaRepository customerRepository
    ) {
        this.organizationService = organizationService;
        this.customerRepository = customerRepository;
    }

    public Customer createCustomer
    (
        UUID organizationId, 
        CustomerRequest customerRequest
    ) {
        if (customerRequest == null) {
            throw new ValidationException("Customer request cannot be null");
        }
        // validate and get organization from organization service
        Organization organization = organizationService.getById(organizationId);

        Customer customer = new Customer();
        customer.setOrganization(organization);
        if (customerRequest.getName() == null) {
            throw new ValidationException("Customer name cannot be null");
        }
        customer.setName(customerRequest.getName());
        if (customerRequest.getEmail() != null) {
            customer.setEmail(customerRequest.getEmail());
        }
        if (customerRequest.getPhone() != null) {
            customer.setPhone(customerRequest.getPhone());
        }
        
        if (customerRequest.getCompanyName() != null) {
            customer.setCompanyName(customerRequest.getCompanyName());
        }
        if (customerRequest.getAddress() != null) {
            customer.setAddress(customerRequest.getAddress());
        }
        customer.activate();
        
        return customerRepository.saveAndFlush(customer);
    }

    public Customer getCustomerById
    (
        UUID organizationId, 
        UUID id
    ) {
        if(id == null) {
            throw new ValidationException("Customer id cannot be null");
        }
        // validate organization from organization service
        organizationService.getById(organizationId);
        return CustomerUtil.validateCustomerWithOrganization(id, organizationId, customerRepository);
    }

    public Page<Customer> getAllCustomers
    (
        UUID organizationId, 
        String name, 
        String email, 
        String phone,
        String companyName, 
        boolean active, 
        Pageable pageable
    ) {
        // validate organization from organization service
        organizationService.getById(organizationId);
        
        Specification<Customer> spec = (root, query, cb) -> {
            Predicate p = cb.equal(root.get("organization").get("id"), organizationId);
            if(name != null && !name.isBlank()) {
                p = cb.and(p, cb.like(root.get("name"), "%" + name + "%"));
            }
            if(email != null && !email.isBlank()) {
                p = cb.and(p, cb.like(root.get("email"), "%" + email + "%"));
            }
            if(phone != null && !phone.isBlank()) {
                p = cb.and(p, cb.like(root.get("phone"), "%" + phone + "%"));
            }
            if(companyName != null && !companyName.isBlank()) {
                p = cb.and(p, cb.like(root.get("companyName"), "%" + companyName + "%"));
            }
            if(active) {
                p = cb.and(p, cb.equal(root.get("active"), active));
            }
            return p;
        };

        return customerRepository.findAll(spec, pageable);
    }

    public Customer updateCustomer
    (
        UUID organizationId, 
        UUID id, 
        CustomerRequest customerRequest
    ) {
       if(customerRequest == null) {
        throw new ValidationException("Customer request cannot be null");
       }
       if(id == null) {
        throw new ValidationException("Customer id cannot be null");
       }
       // validate organization from organization service
       organizationService.getById(organizationId);
       CustomerUtil.validateCustomerWithOrganization(id, organizationId, customerRepository);

       Customer customer = new Customer();
       if(customerRequest.getName() != null) {
        customer.setName(customerRequest.getName());
       }
       if(customerRequest.getEmail() != null) {
        customer.setEmail(customerRequest.getEmail());
       }
       if(customerRequest.getPhone() != null) {
        customer.setPhone(customerRequest.getPhone());
       }
       if(customerRequest.getCompanyName() != null) {
        customer.setCompanyName(customerRequest.getCompanyName());
       }
       if(customerRequest.getAddress() != null) {
        customer.setAddress(customerRequest.getAddress());
       }

       return customerRepository.save(customer);
    }

    public void deleteCustomer
    (
        UUID organizationId, 
        UUID id
    ) {
        if(id == null) {
            throw new ValidationException("Customer id cannot be null");
        }
        // validate organization from organization service
        organizationService.getById(organizationId);
        CustomerUtil.validateCustomerWithOrganization
        (
            id, 
            organizationId,
            customerRepository
        );
        customerRepository.deleteById(id);
    }

    public Customer activateCustomer
    (
        UUID organizationId, 
        UUID id
    ) {
        if(id == null) {
            throw new ValidationException("Customer id cannot be null");
        }
        // validate organization from organization service
        organizationService.getById(organizationId);
        Customer customer = CustomerUtil.validateCustomerWithOrganization
        (
            id, 
            organizationId, 
            customerRepository
        );
        customer.activate();
        return customerRepository.save(customer);
    }

    public Customer deactivateCustomer(UUID organizationId, UUID id) {
        if(id == null) {
            throw new ValidationException("Customer id cannot be null");
        }
        // validate organization from organization service
        organizationService.getById(organizationId);
        Customer customer = CustomerUtil.validateCustomerWithOrganization
        (
            id, 
            organizationId, 
            customerRepository
        );
        customer.deactivate();
        return customerRepository.save(customer);
    }
}
