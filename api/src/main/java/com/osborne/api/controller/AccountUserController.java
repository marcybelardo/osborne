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

import com.osborne.api.dto.AddAccountUserRequest;
import com.osborne.api.dto.UserResponse;
import com.osborne.api.model.User;
import com.osborne.api.service.AccountService;
import com.osborne.api.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/accounts/{accountId}/users")
@RequiredArgsConstructor
public class AccountUserController {

    private final AccountService accountService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<List<UserResponse>> getUsers(@PathVariable UUID accountId) {
        List<UserResponse> users = accountService.getAccountUsers(accountId).stream()
            .map(userService::toResponse)
            .toList();
        return ResponseEntity.ok(users);
    }

    @PostMapping
    public ResponseEntity<Void> addUser(
            @PathVariable UUID accountId,
            @Valid @RequestBody AddAccountUserRequest request) {
        accountService.addUserToAccount(accountId, request.userId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeUser(
            @PathVariable UUID accountId,
            @PathVariable UUID userId) {
        accountService.removeUserFromAccount(accountId, userId);
        return ResponseEntity.noContent().build();
    }
}
