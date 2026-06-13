package com.osborne.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

public record CreateLedgerTransactionRequest(
    @NotNull(message = "Amount is required")
    BigDecimal amount
) {}
