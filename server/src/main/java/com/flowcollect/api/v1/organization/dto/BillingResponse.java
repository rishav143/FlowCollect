package com.flowcollect.api.v1.organization.dto;

import com.flowcollect.domain.organization.OrgPlan;

public record BillingResponse(
    OrgPlan plan
) {}
