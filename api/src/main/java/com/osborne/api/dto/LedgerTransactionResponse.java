package com.osborne.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record LedgerTransactionResponse(
    UUID id,
    BigDecimal amount,
    String description,
    String category,
    LocalDate transactionDate,
    UUID accountId,
    List<UUID> budgetIds,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
