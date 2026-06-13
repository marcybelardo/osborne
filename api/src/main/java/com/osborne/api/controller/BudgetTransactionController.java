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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.osborne.api.dto.AddBudgetTransactionRequest;
import com.osborne.api.dto.LedgerTransactionResponse;
import com.osborne.api.service.BudgetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/budgets/{budgetId}/transactions")
@RequiredArgsConstructor
public class BudgetTransactionController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<Page<LedgerTransactionResponse>> getTransactions(
            @PathVariable UUID budgetId,
            Pageable pageable) {
        return ResponseEntity.ok(budgetService.getBudgetTransactions(budgetId, pageable));
    }

    @PostMapping
    public ResponseEntity<Void> addTransaction(
            @PathVariable UUID budgetId,
            @Valid @RequestBody AddBudgetTransactionRequest request) {
        budgetService.addTransactionToBudget(budgetId, request.transactionId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> removeTransaction(
            @PathVariable UUID budgetId,
            @PathVariable UUID transactionId) {
        budgetService.removeTransactionFromBudget(budgetId, transactionId);
        return ResponseEntity.noContent().build();
    }
}
