package com.osborne.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AddGoalTransactionRequest(
    @NotNull(message = "Transaction ID is required")
    UUID transactionId
) {}
