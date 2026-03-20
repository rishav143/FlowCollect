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
import com.flowcollect.exception.http.ConflictException;
import com.flowcollect.exception.http.ValidationException;
import com.flowcollect.infrastructure.persistence.customer.CustomerJpaRepository;

import jakarta.persistence.criteria.Predicate;
import org.springframework.transaction.annotation.Transactional;

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
        if (customerRequest.getName() == null || customerRequest.getName().isBlank()) {
            throw new ValidationException("Customer name is required");
        }
        customer.setName(customerRequest.getName().trim());
        if (customerRequest.getEmail() != null) {
            String email = customerRequest.getEmail().trim();
            if (email.isBlank()) {
                throw new ValidationException(
                    "Email must not be blank");
            }
            if (!email.equalsIgnoreCase(organization.getEmail())
                    && customerRepository.existsByEmailAndOrganizationId(email, organizationId)) {
                throw new ConflictException(
                    "Customer with email: " + email + " already exists.");
            }
            customer.setEmail(email);
        }
        if (customerRequest.getPhone() != null) {
            String phone = customerRequest.getPhone().trim();
            if (phone.isBlank()) {
                throw new ValidationException(
                    "Phone must not be blank");
            }
            if (!phone.equals(customer.getPhone())
                    && customerRepository.existsByPhoneAndOrganizationId(phone, organizationId)) {
                throw new ConflictException(
                    "Customer with phone: " + phone + " already exists.");
            }
            customer.setPhone(phone);
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
        Boolean active,
        Pageable pageable
    ) {
        // validate organization from organization service
        organizationService.getById(organizationId);

        Specification<Customer> spec = (root, query, cb) -> {
            Predicate p = cb.equal(root.get("organization").get("id"), organizationId);
            if(name != null && !name.isBlank()) {
                p = cb.and(p, cb.like(cb.lower(root.get("name")), "%" + name.trim().toLowerCase() + "%"));
            }
            if(email != null && !email.isBlank()) {
                p = cb.and(p, cb.like(cb.lower(root.get("email")), "%" + email.trim().toLowerCase() + "%"));
            }
            if(phone != null && !phone.isBlank()) {
                p = cb.and(p, cb.like(root.get("phone"), "%" + phone.trim() + "%"));
            }
            if(companyName != null && !companyName.isBlank()) {
                p = cb.and(p, cb.like(cb.lower(root.get("companyName")), "%" + companyName.trim().toLowerCase() + "%"));
            }
            if(active != null) {
                p = cb.and(p, cb.equal(root.get("active"), active));
            }
            return p;
        };

        return customerRepository.findAll(spec, pageable);
    }

    @Transactional
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
       Customer customer = CustomerUtil.validateCustomerWithOrganization(id, organizationId, customerRepository);

       if(customerRequest.getName() != null) {
           String name = customerRequest.getName().trim();
           if(name.isBlank()) {
               throw new ValidationException("Customer name must not be blank");
           }
           customer.setName(name);
       }
       if(customerRequest.getEmail() != null) {
           String email = customerRequest.getEmail().trim();
           if(email.isBlank()) {
               throw new ValidationException("Email must not be blank");
           }
           if(!email.equalsIgnoreCase(customer.getEmail())
                   && customerRepository.existsByEmailAndOrganizationId(email, organizationId)) {
               throw new ConflictException("Customer with email: " + email + " already exists.");
           }
           customer.setEmail(email);
       }
       if(customerRequest.getPhone() != null) {
           String phone = customerRequest.getPhone().trim();
           if(phone.isBlank()) {
               throw new ValidationException("Phone must not be blank");
           }
           if(!phone.equals(customer.getPhone())
                   && customerRepository.existsByPhoneAndOrganizationId(phone, organizationId)) {
               throw new ConflictException("Customer with phone: " + phone + " already exists.");
           }
           customer.setPhone(phone);
       }
       if(customerRequest.getCompanyName() != null) {
           customer.setCompanyName(customerRequest.getCompanyName().trim());
       }
       if(customerRequest.getAddress() != null) {
           customer.setAddress(customerRequest.getAddress().trim());
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
