package com.osborne.api.dto;

import java.math.BigDecimal;

public record UpdateLedgerTransactionRequest(
    BigDecimal amount
) {}
