package com.flowcollect.api.v1.invoice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class TaxLineRequest {

    @NotBlank(message = "Tax line label must not be blank")
    @Size(max = 50, message = "Tax line label must not exceed 50 characters")
    private String label;

    @NotNull(message = "Tax line amount is required")
    @DecimalMin(value = "0.00", message = "Tax line amount must be >= 0")
    private BigDecimal amount;

    public String getLabel()              { return label; }
    public void setLabel(String label)    { this.label = label; }
    public BigDecimal getAmount()         { return amount; }
    public void setAmount(BigDecimal a)   { this.amount = a; }
}
