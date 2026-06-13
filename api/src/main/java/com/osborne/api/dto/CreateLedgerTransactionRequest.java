package com.osborne.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record CreateLedgerTransactionRequest(
    @NotNull(message = "Amount is required")
    BigDecimal amount,

    String description,

    String category,

    LocalDate transactionDate
) {}
