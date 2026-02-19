package com.cashclarity.infrastructure.persistence.invoice;

import com.cashclarity.domain.invoice.followup.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FollowUpJpaRepository extends JpaRepository<FollowUp, UUID>, JpaSpecificationExecutor<FollowUp> {
    // Additional query methods can be added as needed.
    
}

