package com.osborne.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record CreateLedgerTransactionRequest(
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "-999999999.99", message = "Amount must be at least -999,999,999.99")
    @DecimalMax(value = "999999999.99", message = "Amount must not exceed 999,999,999.99")
    BigDecimal amount,

    String description,

    String category,

    LocalDate transactionDate
) {}
