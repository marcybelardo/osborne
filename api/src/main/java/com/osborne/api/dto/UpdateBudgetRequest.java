package com.osborne.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record UpdateBudgetRequest(
    @DecimalMin(value = "0.01", message = "Budget amount must be positive")
    @DecimalMax(value = "999999999.99", message = "Amount must not exceed 999,999,999.99")
    BigDecimal amount
) {}
