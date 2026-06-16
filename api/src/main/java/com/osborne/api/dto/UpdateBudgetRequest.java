package com.osborne.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.osborne.api.enums.BudgetTimeframe;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record UpdateBudgetRequest(
    @Size(max = 200, message = "Name must not exceed 200 characters")
    String name,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    BudgetTimeframe timeframe,

    LocalDate startDate,

    LocalDate endDate,

    @DecimalMin(value = "0.01", message = "Budget amount must be positive")
    @DecimalMax(value = "999999999.99", message = "Amount must not exceed 999,999,999.99")
    BigDecimal amount
) {}
