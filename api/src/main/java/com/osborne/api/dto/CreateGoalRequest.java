package com.osborne.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateGoalRequest(
    @NotBlank(message = "Goal name is required")
    String name,

    @NotNull(message = "Target amount is required")
    @DecimalMin(value = "0.01", message = "Target amount must be positive")
    BigDecimal targetAmount,

    LocalDate targetDate
) {}
