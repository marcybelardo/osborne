package com.osborne.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AddBudgetUserRequest(
    @NotNull(message = "User ID is required")
    UUID userId
) {}
