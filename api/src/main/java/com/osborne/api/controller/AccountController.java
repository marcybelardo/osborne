package com.osborne.api.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.osborne.api.dto.*;
import com.osborne.api.model.Account;
import com.osborne.api.service.AccountService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public ResponseEntity<Page<Account>> getAccounts(Pageable pageable) {
	return ResponseEntity.ok(accountService.getAccountsForCurrentUser(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable UUID id) {
	return ResponseEntity.ok(accountService.getAccountById(id));
    }

    @PostMapping
    public ResponseEntity<Account> createAccount(
	@Valid @RequestBody CreateAccountRequest request
    ) {
	return new ResponseEntity<>(
	    accountService.createAccount(request),
	    HttpStatus.CREATED
	);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Account> updateAccount(
	@PathVariable UUID id,
	@RequestBody UpdateAccountRequest request
    ) {
	return ResponseEntity.ok(accountService.updateAccount(id, request));
    }

}
