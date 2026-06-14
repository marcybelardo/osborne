package com.osborne.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;

public record UpdateGoalRequest(
    String name,
    @DecimalMin(value = "0.01", message = "Target amount must be positive")
    BigDecimal targetAmount,
    LocalDate targetDate
) {}
