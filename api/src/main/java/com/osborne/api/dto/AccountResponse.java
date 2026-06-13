package com.osborne.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.osborne.api.enums.AccountType;

public record AccountResponse(
    UUID id,
    String name,
    AccountType type,
    String currency,
    BigDecimal initialBalance,
    BigDecimal currentBalance,
    List<UUID> userIds,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
