package com.osborne.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateLedgerTransactionRequest(
    BigDecimal amount,
    String description,
    String category,
    LocalDate transactionDate
) {}
