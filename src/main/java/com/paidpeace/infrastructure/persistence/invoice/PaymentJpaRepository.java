package com.paidpeace.infrastructure.persistence.invoice;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.paidpeace.domain.invoice.payment.Payment;

@Repository
    public interface PaymentJpaRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {
        
    }