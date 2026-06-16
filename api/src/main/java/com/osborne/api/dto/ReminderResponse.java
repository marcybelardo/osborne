package com.osborne.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import com.osborne.api.enums.ReminderStatus;
import com.osborne.api.enums.ReminderType;

public record ReminderResponse(
    UUID id,
    String message,
    ReminderStatus status,
    ReminderType type,
    UUID userId,
    UUID transactionId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
