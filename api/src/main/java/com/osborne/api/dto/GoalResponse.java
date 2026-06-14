package com.osborne.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record GoalResponse(
    UUID id,
    String name,
    BigDecimal targetAmount,
    BigDecimal currentAmount,
    LocalDate targetDate,
    double progressPercent,
    List<UUID> userIds,
    List<UUID> transactionIds,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
