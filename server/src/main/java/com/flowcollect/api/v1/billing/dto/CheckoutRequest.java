package com.flowcollect.api.v1.billing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class CheckoutRequest {

    @NotNull
    @Pattern(regexp = "PRO", message = "Only PRO plan checkout is supported")
    private String plan;

    public String getPlan() { return plan; }
    public void   setPlan(String plan) { this.plan = plan; }
}
