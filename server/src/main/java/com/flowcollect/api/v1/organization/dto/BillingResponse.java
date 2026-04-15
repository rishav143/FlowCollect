package com.flowcollect.api.v1.organization.dto;

import com.flowcollect.domain.organization.OrgPlan;
import com.flowcollect.domain.organization.OrganizationStatus;

import java.time.Instant;

public record BillingResponse(
    OrgPlan            plan,
    OrganizationStatus orgStatus,
    Instant            planExpiresAt,
    boolean            subscriptionActive,
    long               activeInvoiceCount
) {}
