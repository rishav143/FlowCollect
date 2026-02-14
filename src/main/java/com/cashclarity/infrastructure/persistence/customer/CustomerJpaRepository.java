package com.cashclarity.infrastructure.persistence.customer;

import com.cashclarity.domain.customer.Customer;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerJpaRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByIdAndOrganizationId(Long id, Long organizationId);
}
