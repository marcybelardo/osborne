package com.osborne.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String displayName,
    String email,
    LocalDateTime createdAt
) {}
