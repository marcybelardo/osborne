package com.osborne.api.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.osborne.api.dto.AddGoalTransactionRequest;
import com.osborne.api.dto.LedgerTransactionResponse;
import com.osborne.api.model.Budget;
import com.osborne.api.model.Goal;
import com.osborne.api.model.LedgerTransaction;
import com.osborne.api.service.GoalService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/goals/{goalId}/transactions")
@RequiredArgsConstructor
public class GoalTransactionController {

    private final GoalService goalService;

    @GetMapping
    public ResponseEntity<List<LedgerTransactionResponse>> getTransactions(
            @PathVariable UUID goalId) {
        List<LedgerTransactionResponse> transactions = goalService.getGoalTransactions(goalId)
            .stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.ok(transactions);
    }

    @PostMapping
    public ResponseEntity<Void> addTransaction(
            @PathVariable UUID goalId,
            @Valid @RequestBody AddGoalTransactionRequest request) {
        goalService.addTransactionToGoal(goalId, request.transactionId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{transactionId}")
    public ResponseEntity<Void> removeTransaction(
            @PathVariable UUID goalId,
            @PathVariable UUID transactionId) {
        goalService.removeTransactionFromGoal(goalId, transactionId);
        return ResponseEntity.noContent().build();
    }

    private LedgerTransactionResponse toResponse(LedgerTransaction tx) {
        return new LedgerTransactionResponse(
            tx.getId(),
            tx.getAmount(),
            tx.getDescription(),
            tx.getCategory(),
            tx.getTransactionDate(),
            tx.getAccount().getId(),
            tx.getBudgets().stream().map(Budget::getId).toList(),
            tx.getGoals().stream().map(Goal::getId).toList(),
            tx.getCreatedAt(),
            tx.getUpdatedAt()
        );
    }
}
