package com.osborne.api.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.osborne.api.dto.CreateLedgerTransactionRequest;
import com.osborne.api.dto.LedgerTransactionResponse;
import com.osborne.api.dto.UpdateLedgerTransactionRequest;
import com.osborne.api.service.LedgerTransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/accounts/{accountId}/transactions")
@RequiredArgsConstructor
public class LedgerTransactionController {

    private final LedgerTransactionService ledgerTransactionService;

    @GetMapping
    public ResponseEntity<Page<LedgerTransactionResponse>> getTransactions(
            @PathVariable UUID accountId,
            Pageable pageable) {
        return ResponseEntity.ok(ledgerTransactionService.getTransactionsForAccount(accountId, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LedgerTransactionResponse> getTransaction(
            @PathVariable UUID accountId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(ledgerTransactionService.getTransactionById(accountId, id));
    }

    @PostMapping
    public ResponseEntity<LedgerTransactionResponse> createTransaction(
            @PathVariable UUID accountId,
            @Valid @RequestBody CreateLedgerTransactionRequest request) {
        return new ResponseEntity<>(
            ledgerTransactionService.createTransaction(accountId, request),
            HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LedgerTransactionResponse> updateTransaction(
            @PathVariable UUID accountId,
            @PathVariable UUID id,
            @RequestBody UpdateLedgerTransactionRequest request) {
        return ResponseEntity.ok(ledgerTransactionService.updateTransaction(accountId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(
            @PathVariable UUID accountId,
            @PathVariable UUID id) {
        ledgerTransactionService.deleteTransaction(accountId, id);
        return ResponseEntity.noContent().build();
    }
}
