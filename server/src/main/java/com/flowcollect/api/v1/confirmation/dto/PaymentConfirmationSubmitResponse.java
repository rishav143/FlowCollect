package com.flowcollect.api.v1.confirmation.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Returned to the customer after they successfully submit a payment claim.
 */
public class PaymentConfirmationSubmitResponse {

    private UUID confirmationId;
    private BigDecimal amountClaimed;
    private String status;
    private Instant createdAt;

    public UUID getConfirmationId()    { return confirmationId; }
    public BigDecimal getAmountClaimed() { return amountClaimed; }
    public String getStatus()          { return status; }
    public Instant getCreatedAt()      { return createdAt; }

    public void setConfirmationId(UUID confirmationId)     { this.confirmationId = confirmationId; }
    public void setAmountClaimed(BigDecimal amountClaimed) { this.amountClaimed = amountClaimed; }
    public void setStatus(String status)                   { this.status = status; }
    public void setCreatedAt(Instant createdAt)            { this.createdAt = createdAt; }
}
