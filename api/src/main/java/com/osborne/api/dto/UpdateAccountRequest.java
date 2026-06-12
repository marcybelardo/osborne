package com.osborne.api.dto;

import java.util.UUID;

import com.osborne.api.enums.AccountType;

public record UpdateAccountRequest(
    String name,
    AccountType type,
    String currency,

    // if an id from an update request is not in the user list, assume add user
    // if an id from an update request is in the user list, assume remove user
    UUID userId
) {}
