package com.paidpeace.api.v1.invoice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class InvoiceItemRequest {

    @NotBlank(message = "description must not be blank")
    @Size(max = 255, message = "description must not exceed 255 characters")
    private String description;

    @Positive(message = "quantity must be positive")
    private int quantity;

    @NotNull(message = "unitPrice is required")
    @DecimalMin(value = "0.00", message = "unitPrice must be greater than or equal to 0")
    private BigDecimal unitPrice;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
