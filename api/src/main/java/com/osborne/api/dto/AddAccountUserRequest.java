package com.osborne.api.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AddAccountUserRequest(
    @NotNull(message = "userId is required")
    UUID userId
) {}
