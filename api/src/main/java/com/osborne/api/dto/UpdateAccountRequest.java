package com.osborne.api.dto;

import com.osborne.api.enums.AccountType;

public record UpdateAccountRequest(
    String name,
    AccountType type,
    String currency
) {}
