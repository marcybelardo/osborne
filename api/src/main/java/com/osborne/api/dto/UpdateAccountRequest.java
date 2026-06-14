package com.osborne.api.dto;

import com.osborne.api.enums.AccountType;

import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
    @Size(min = 1, max = 200, message = "Account name must be between 1 and 200 characters")
    String name,
    AccountType type,
    String currency
) {}
