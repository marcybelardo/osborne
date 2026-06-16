package com.osborne.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.osborne.api.enums.BudgetTimeframe;

public record BudgetResponse(
    UUID id,
    String name,
    String description,
    BudgetTimeframe timeframe,
    LocalDate startDate,
    LocalDate endDate,
    LocalDate periodStart,
    LocalDate periodEnd,
    String periodLabel,
    BigDecimal amount,
    BigDecimal currentSpending,
    List<UserSummary> users,
    List<UUID> transactionIds,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
