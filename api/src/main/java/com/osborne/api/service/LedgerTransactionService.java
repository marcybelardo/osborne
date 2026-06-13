package com.osborne.api.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.osborne.api.dto.CreateLedgerTransactionRequest;
import com.osborne.api.dto.UpdateLedgerTransactionRequest;
import com.osborne.api.model.Account;
import com.osborne.api.model.LedgerTransaction;
import com.osborne.api.model.User;
import com.osborne.api.repository.AccountRepository;
import com.osborne.api.repository.LedgerTransactionRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LedgerTransactionService {

    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final AccountRepository accountRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Page<LedgerTransaction> getTransactionsForAccount(UUID accountId, Pageable pageable) {
        Account account = getAccountAndVerifyAccess(accountId);
        return ledgerTransactionRepository.findByAccount(account, pageable);
    }

    @Transactional(readOnly = true)
    public LedgerTransaction getTransactionById(UUID accountId, UUID transactionId) {
        getAccountAndVerifyAccess(accountId);
        LedgerTransaction transaction = ledgerTransactionRepository.findById(transactionId)
            .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + transactionId));

        if (!transaction.getAccount().getId().equals(accountId)) {
            throw new EntityNotFoundException("Transaction not found with id: " + transactionId);
        }

        return transaction;
    }

    @Transactional
    public LedgerTransaction createTransaction(UUID accountId, CreateLedgerTransactionRequest request) {
        Account account = getAccountAndVerifyAccess(accountId);

        LedgerTransaction transaction = LedgerTransaction.builder()
            .amount(request.amount())
            .account(account)
            .build();

        return ledgerTransactionRepository.save(transaction);
    }

    @Transactional
    public LedgerTransaction updateTransaction(
            UUID accountId, UUID transactionId, UpdateLedgerTransactionRequest request) {
        getAccountAndVerifyAccess(accountId);
        LedgerTransaction transaction = ledgerTransactionRepository.findById(transactionId)
            .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + transactionId));

        if (!transaction.getAccount().getId().equals(accountId)) {
            throw new EntityNotFoundException("Transaction not found with id: " + transactionId);
        }

        if (request.amount() != null) {
            transaction.setAmount(request.amount());
        }

        return ledgerTransactionRepository.save(transaction);
    }

    @Transactional
    public void deleteTransaction(UUID accountId, UUID transactionId) {
        getAccountAndVerifyAccess(accountId);
        LedgerTransaction transaction = ledgerTransactionRepository.findById(transactionId)
            .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + transactionId));

        if (!transaction.getAccount().getId().equals(accountId)) {
            throw new EntityNotFoundException("Transaction not found with id: " + transactionId);
        }

        ledgerTransactionRepository.delete(transaction);
    }

    private Account getAccountAndVerifyAccess(UUID accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new EntityNotFoundException("Account not found with id: " + accountId));

        User currentUser = getCurrentUser();
        if (!account.getUsers().contains(currentUser)) {
            throw new AccessDeniedException("User does not manage this account");
        }

        return account;
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getUserByEmail(email);
    }
}
