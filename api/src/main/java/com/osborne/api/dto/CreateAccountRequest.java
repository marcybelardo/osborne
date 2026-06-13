package com.osborne.api.dto;

import java.math.BigDecimal;

import com.osborne.api.enums.AccountType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAccountRequest(
    @NotBlank String name,
    @NotNull AccountType type,
    String currency,
    BigDecimal initialBalance
) {}
