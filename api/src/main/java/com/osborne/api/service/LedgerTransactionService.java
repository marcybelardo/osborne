package com.osborne.api.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.osborne.api.dto.CreateLedgerTransactionRequest;
import com.osborne.api.dto.LedgerTransactionResponse;
import com.osborne.api.dto.UpdateLedgerTransactionRequest;
import com.osborne.api.model.Account;
import com.osborne.api.model.Budget;
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
    public Page<LedgerTransactionResponse> getTransactionsForAccount(UUID accountId, Pageable pageable) {
        Account account = getAccountAndVerifyAccess(accountId);
        return ledgerTransactionRepository.findByAccount(account, pageable)
            .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public LedgerTransactionResponse getTransactionById(UUID accountId, UUID transactionId) {
        getAccountAndVerifyAccess(accountId);
        LedgerTransaction transaction = ledgerTransactionRepository.findById(transactionId)
            .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + transactionId));

        if (!transaction.getAccount().getId().equals(accountId)) {
            throw new EntityNotFoundException("Transaction not found with id: " + transactionId);
        }

        return toResponse(transaction);
    }

    @Transactional
    public LedgerTransactionResponse createTransaction(UUID accountId, CreateLedgerTransactionRequest request) {
        Account account = getAccountAndVerifyAccess(accountId);

        LedgerTransaction transaction = LedgerTransaction.builder()
            .amount(request.amount())
            .description(request.description())
            .category(request.category())
            .transactionDate(request.transactionDate() != null ? request.transactionDate() : java.time.LocalDate.now())
            .account(account)
            .build();

        LedgerTransaction saved = ledgerTransactionRepository.save(transaction);
        return toResponse(saved);
    }

    @Transactional
    public LedgerTransactionResponse updateTransaction(
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
        if (request.description() != null) {
            transaction.setDescription(request.description());
        }
        if (request.category() != null) {
            transaction.setCategory(request.category());
        }
        if (request.transactionDate() != null) {
            transaction.setTransactionDate(request.transactionDate());
        }

        LedgerTransaction saved = ledgerTransactionRepository.save(transaction);
        return toResponse(saved);
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

    private LedgerTransactionResponse toResponse(LedgerTransaction transaction) {
        List<UUID> budgetIds = transaction.getBudgets().stream()
            .map(Budget::getId)
            .toList();

        return new LedgerTransactionResponse(
            transaction.getId(),
            transaction.getAmount(),
            transaction.getDescription(),
            transaction.getCategory(),
            transaction.getTransactionDate(),
            transaction.getAccount().getId(),
            budgetIds,
            transaction.getCreatedAt(),
            transaction.getUpdatedAt()
        );
    }

}
