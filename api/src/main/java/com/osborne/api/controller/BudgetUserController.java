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

import com.osborne.api.dto.AddBudgetUserRequest;
import com.osborne.api.dto.UserResponse;
import com.osborne.api.service.BudgetService;
import com.osborne.api.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/budgets/{budgetId}/users")
@RequiredArgsConstructor
public class BudgetUserController {

    private final BudgetService budgetService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers(@PathVariable UUID budgetId) {
        List<UserResponse> users = budgetService.getBudgetUsers(budgetId).stream()
            .map(userService::toResponse)
            .toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<Void> addUser(
            @PathVariable UUID budgetId,
            @Valid @RequestBody AddBudgetUserRequest request) {
        budgetService.addUserToBudget(budgetId, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeUser(
            @PathVariable UUID budgetId,
            @PathVariable UUID userId) {
        budgetService.removeUserFromBudget(budgetId, userId);
        return ResponseEntity.noContent().build();
    }
}
