package com.cashclarity.infrastructure.persistence.customer;

import com.cashclarity.domain.customer.Customer;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerJpaRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByIdAndOrganizationId(UUID id, java.util.UUID organizationId);
}
