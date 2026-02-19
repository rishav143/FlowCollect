package com.cashclarity.infrastructure.persistence.invoice;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.cashclarity.domain.invoice.payment.Payment;

@Repository
    public interface PaymentJpaRepository extends JpaRepository<Payment, UUID>, JpaSpecificationExecutor<Payment> {
        
    }