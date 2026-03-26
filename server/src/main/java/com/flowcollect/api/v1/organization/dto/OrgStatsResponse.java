package com.flowcollect.api.v1.organization.dto;

import java.math.BigDecimal;

public record OrgStatsResponse(
    BigDecimal collectedThisMonth
) {}
