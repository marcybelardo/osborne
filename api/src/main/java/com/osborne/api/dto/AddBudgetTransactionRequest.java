package com.osborne.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AddBudgetTransactionRequest(
    @NotNull(message = "transactionId is required")
    UUID transactionId
) {}
