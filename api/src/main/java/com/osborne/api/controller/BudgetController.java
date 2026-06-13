package com.osborne.api.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.osborne.api.dto.CreateBudgetRequest;
import com.osborne.api.dto.UpdateBudgetRequest;
import com.osborne.api.model.Budget;
import com.osborne.api.service.BudgetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    @GetMapping
    public ResponseEntity<Page<Budget>> getBudgets(Pageable pageable) {
        return ResponseEntity.ok(budgetService.getBudgetsForCurrentUser(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Budget> getBudget(@PathVariable UUID id) {
        return ResponseEntity.ok(budgetService.getBudgetById(id));
    }

    @PostMapping
    public ResponseEntity<Budget> createBudget(@Valid @RequestBody CreateBudgetRequest request) {
        return new ResponseEntity<>(budgetService.createBudget(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Budget> updateBudget(
            @PathVariable UUID id,
            @RequestBody UpdateBudgetRequest request) {
        return ResponseEntity.ok(budgetService.updateBudget(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBudget(@PathVariable UUID id) {
        budgetService.deleteBudget(id);
        return ResponseEntity.noContent().build();
    }
}
