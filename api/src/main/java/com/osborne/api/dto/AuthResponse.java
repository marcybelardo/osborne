package com.osborne.api.dto;

public record AuthResponse(
    String token,
    String refreshToken,
    String displayName,
    String id
) {}
