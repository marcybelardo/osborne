package com.osborne.api.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.osborne.api.dto.CreateLedgerTransactionRequest;
import com.osborne.api.dto.LedgerTransactionResponse;
import com.osborne.api.dto.UpdateLedgerTransactionRequest;
import com.osborne.api.model.Account;
import com.osborne.api.model.Budget;
import com.osborne.api.model.Goal;
import com.osborne.api.model.LedgerTransaction;
import com.osborne.api.model.User;
import com.osborne.api.repository.AccountRepository;
import com.osborne.api.repository.BudgetRepository;
import com.osborne.api.repository.GoalRepository;
import com.osborne.api.repository.LedgerTransactionRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LedgerTransactionService {

    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final AccountRepository accountRepository;
    private final BudgetRepository budgetRepository;
    private final GoalRepository goalRepository;
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

        if (request.amount().compareTo(BigDecimal.ZERO) == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction amount must not be zero");
        }

        LedgerTransaction transaction = LedgerTransaction.builder()
            .amount(request.amount())
            .description(request.description())
            .category(request.category())
            .transactionDate(request.transactionDate() != null ? request.transactionDate() : java.time.LocalDate.now())
            .account(account)
            .build();

        LedgerTransaction saved = ledgerTransactionRepository.save(transaction);

        // Allocate to budgets
        for (UUID budgetId : request.budgetIds()) {
            Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + budgetId));
            User currentUser = getCurrentUser();
            if (!budget.getUsers().contains(currentUser)) {
                throw new AccessDeniedException("User does not manage budget: " + budgetId);
            }
            budget.getTransactions().add(saved);
            budgetRepository.save(budget);
        }

        // Allocate to goals
        for (UUID goalId : request.goalIds()) {
            Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new EntityNotFoundException("Goal not found with id: " + goalId));
            User currentUser = getCurrentUser();
            if (!goal.getUsers().contains(currentUser)) {
                throw new AccessDeniedException("User does not manage goal: " + goalId);
            }
            goal.getTransactions().add(saved);
            goalRepository.save(goal);
        }

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
            if (request.amount().compareTo(BigDecimal.ZERO) == 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction amount must not be zero");
            }
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

        // Re-allocate budgets if provided
        if (request.budgetIds() != null) {
            // Clear current budget allocations
            for (Budget budget : saved.getBudgets()) {
                budget.getTransactions().remove(saved);
                budgetRepository.save(budget);
            }
            saved.getBudgets().clear();

            // Add new budget allocations
            User currentUser = getCurrentUser();
            for (UUID budgetId : request.budgetIds()) {
                Budget budget = budgetRepository.findById(budgetId)
                    .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + budgetId));
                if (!budget.getUsers().contains(currentUser)) {
                    throw new AccessDeniedException("User does not manage budget: " + budgetId);
                }
                budget.getTransactions().add(saved);
                saved.getBudgets().add(budget);
                budgetRepository.save(budget);
            }
        }

        // Re-allocate goals if provided
        if (request.goalIds() != null) {
            // Clear current goal allocations
            for (Goal goal : saved.getGoals()) {
                goal.getTransactions().remove(saved);
                goalRepository.save(goal);
            }
            saved.getGoals().clear();

            // Add new goal allocations
            User currentUser = getCurrentUser();
            for (UUID goalId : request.goalIds()) {
                Goal goal = goalRepository.findById(goalId)
                    .orElseThrow(() -> new EntityNotFoundException("Goal not found with id: " + goalId));
                if (!goal.getUsers().contains(currentUser)) {
                    throw new AccessDeniedException("User does not manage goal: " + goalId);
                }
                goal.getTransactions().add(saved);
                saved.getGoals().add(goal);
                goalRepository.save(goal);
            }
        }

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

        List<UUID> goalIds = transaction.getGoals().stream()
            .map(Goal::getId)
            .toList();

        return new LedgerTransactionResponse(
            transaction.getId(),
            transaction.getAmount(),
            transaction.getDescription(),
            transaction.getCategory(),
            transaction.getTransactionDate(),
            transaction.getAccount().getId(),
            budgetIds,
            goalIds,
            transaction.getCreatedAt(),
            transaction.getUpdatedAt()
        );
    }

}
