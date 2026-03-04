package com.paidpeace.api.v1.customer;

import org.springframework.web.bind.annotation.RestController;

import com.paidpeace.api.v1.customer.dto.CustomerMapper;
import com.paidpeace.api.v1.customer.dto.CustomerRequest;
import com.paidpeace.api.v1.customer.dto.CustomerResponse;
import com.paidpeace.application.customer.CustomerService;
import com.paidpeace.domain.customer.Customer;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/customers")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }
    
    // Create Customer
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer
    (
        @PathVariable UUID organizationId,
        @RequestBody @Valid CustomerRequest customerRequest
    ) {
        Customer customer = customerService.createCustomer(organizationId, customerRequest);
        return ResponseEntity.ok(CustomerMapper.toResponse(customer));
    }

    // Get Customer by ID
    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponse> getCustomerById
    (
        @PathVariable UUID organizationId,
        @PathVariable UUID id
    ) {
        Customer customer = customerService.getCustomerById(organizationId, id);
        return ResponseEntity.ok(CustomerMapper.toResponse(customer));
    }

    // Get All Customers
    @GetMapping
    public ResponseEntity<Page<CustomerResponse>> getAllCustomers
    (
        @PathVariable UUID organizationId,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) String companyName,
        @RequestParam(required = false) boolean active,
        Pageable pageable
    ) {
        Page<Customer> customers = customerService.getAllCustomers(organizationId, name, email, phone, companyName, active, pageable);
        return ResponseEntity.ok(customers.map(CustomerMapper::toResponse));
    }

    // Update Customer
    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponse> updateCustomer
    (
        @PathVariable UUID organizationId,
        @PathVariable UUID id,
        @RequestBody @Valid CustomerRequest customerRequest
    ) {
        Customer customer = customerService.updateCustomer(organizationId, id, customerRequest);
        return ResponseEntity.ok(CustomerMapper.toResponse(customer));
    }

    // Delete Customer
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCustomer
    (
        @PathVariable UUID organizationId,
        @PathVariable UUID id
    ) {
        customerService.deleteCustomer(organizationId, id);
        return ResponseEntity.noContent().build();
    }

    // Activate Customer
    @PutMapping("/{id}/activate")
    public ResponseEntity<CustomerResponse> activateCustomer
    (
        @PathVariable UUID organizationId,
        @PathVariable UUID id
    ) {
        Customer customer = customerService.activateCustomer(organizationId, id);
        return ResponseEntity.ok(CustomerMapper.toResponse(customer));
    }

    // Deactivate Customer
    @PutMapping("/{id}/deactivate")
    public ResponseEntity<CustomerResponse> deactivateCustomer
    (
        @PathVariable UUID organizationId,
        @PathVariable UUID id
    ) {
        Customer customer = customerService.deactivateCustomer(organizationId, id);
        return ResponseEntity.ok(CustomerMapper.toResponse(customer));
    }
}
 