package com.osborne.api.dto;

import java.util.UUID;

public record UserSummary(
    UUID id,
    String displayName
) {}
