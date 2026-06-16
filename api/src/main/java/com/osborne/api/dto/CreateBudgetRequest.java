package com.osborne.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.osborne.api.enums.BudgetTimeframe;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateBudgetRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    String name,

    @Size(max = 500, message = "Description must not exceed 500 characters")
    String description,

    @NotNull(message = "Timeframe is required")
    BudgetTimeframe timeframe,

    LocalDate startDate,

    LocalDate endDate,

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    BigDecimal amount
) {}
