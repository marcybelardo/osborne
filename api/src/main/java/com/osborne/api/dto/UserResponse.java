package com.osborne.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.osborne.api.enums.UserRole;

public record UserResponse(
    UUID id,
    String displayName,
    String email,
    UserRole role,
    LocalDateTime createdAt
) {}
