package com.osborne.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record UpdateLedgerTransactionRequest(
    @DecimalMin(value = "-999999999.99", message = "Amount must be at least -999,999,999.99")
    @DecimalMax(value = "999999999.99", message = "Amount must not exceed 999,999,999.99")
    BigDecimal amount,
    String description,
    String category,
    LocalDate transactionDate,
    List<UUID> budgetIds,
    List<UUID> goalIds
) {}
