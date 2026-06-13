package com.osborne.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record BudgetResponse(
    UUID id,
    BigDecimal amount,
    BigDecimal currentSpending,
    UUID createdByUserId,
    List<UUID> transactionIds,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
