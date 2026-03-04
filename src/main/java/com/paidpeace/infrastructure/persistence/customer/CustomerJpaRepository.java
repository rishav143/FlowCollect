package com.paidpeace.infrastructure.persistence.customer;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.paidpeace.domain.customer.Customer;

@Repository
public interface CustomerJpaRepository extends JpaRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {

    Optional<Customer> findByIdAndOrganizationId(UUID id, java.util.UUID organizationId);
}
