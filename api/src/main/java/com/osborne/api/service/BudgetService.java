package com.osborne.api.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.osborne.api.dto.BudgetResponse;
import com.osborne.api.dto.CreateBudgetRequest;
import com.osborne.api.dto.LedgerTransactionResponse;
import com.osborne.api.dto.UpdateBudgetRequest;
import com.osborne.api.model.Budget;
import com.osborne.api.model.Goal;
import com.osborne.api.model.LedgerTransaction;
import com.osborne.api.model.User;
import com.osborne.api.repository.BudgetRepository;
import com.osborne.api.repository.LedgerTransactionRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Page<BudgetResponse> getBudgetsForCurrentUser(Pageable pageable) {
        User currentUser = getCurrentUser();
        // TODO: N+1 — currentSpending computed per budget. Acceptable for
        // paginated results. Consider a custom query for batching.
        return budgetRepository.findByUsersContaining(currentUser, pageable)
            .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public BudgetResponse getBudgetById(UUID id) {
        Budget budget = budgetRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + id));
        verifyAccess(budget);
        return toResponse(budget);
    }

    @Transactional
    public BudgetResponse createBudget(CreateBudgetRequest request) {
        User currentUser = getCurrentUser();
        Budget budget = Budget.builder()
            .amount(request.amount())
            .users(new HashSet<>(Set.of(currentUser)))
            .build();
        Budget saved = budgetRepository.save(budget);
        return toResponse(saved);
    }

    @Transactional
    public BudgetResponse updateBudget(UUID id, UpdateBudgetRequest request) {
        Budget budget = budgetRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + id));
        verifyAccess(budget);

        if (request.amount() != null) {
            budget.setAmount(request.amount());
        }

        Budget saved = budgetRepository.save(budget);
        return toResponse(saved);
    }

    @Transactional
    public void deleteBudget(UUID id) {
        Budget budget = budgetRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + id));
        verifyAccess(budget);
        budgetRepository.delete(budget);
    }

    @Transactional(readOnly = true)
    public Page<LedgerTransactionResponse> getBudgetTransactions(UUID budgetId, Pageable pageable) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + budgetId));
        verifyAccess(budget);
        return ledgerTransactionRepository.findByBudgetsId(budgetId, pageable)
            .map(this::toTransactionResponse);
    }

    @Transactional
    public void addTransactionToBudget(UUID budgetId, UUID transactionId) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + budgetId));
        verifyAccess(budget);

        LedgerTransaction transaction = ledgerTransactionRepository.findById(transactionId)
            .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + transactionId));

        budget.getTransactions().add(transaction);
        budgetRepository.save(budget);
    }

    @Transactional
    public void removeTransactionFromBudget(UUID budgetId, UUID transactionId) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + budgetId));
        verifyAccess(budget);

        boolean removed = budget.getTransactions()
            .removeIf(t -> t.getId().equals(transactionId));

        if (!removed) {
            throw new EntityNotFoundException("Transaction not found in budget");
        }

        budgetRepository.save(budget);
    }

    @Transactional(readOnly = true)
    public List<User> getBudgetUsers(UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + budgetId));
        verifyAccess(budget);
        return List.copyOf(budget.getUsers());
    }

    @Transactional
    public void addUserToBudget(UUID budgetId, UUID userId) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + budgetId));
        verifyAccess(budget);

        User targetUser = userService.getUserById(userId);
        budget.getUsers().add(targetUser);
        budgetRepository.save(budget);
    }

    @Transactional
    public void removeUserFromBudget(UUID budgetId, UUID userId) {
        Budget budget = budgetRepository.findById(budgetId)
            .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + budgetId));
        verifyAccess(budget);

        if (budget.getUsers().size() <= 1) {
            throw new IllegalStateException("Budget must have at least one manager");
        }

        boolean removed = budget.getUsers()
            .removeIf(u -> u.getId().equals(userId));

        if (!removed) {
            throw new EntityNotFoundException("User not found on budget");
        }

        budgetRepository.save(budget);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getUserByEmail(email);
    }

    private void verifyAccess(Budget budget) {
        User currentUser = getCurrentUser();
        if (!budget.getUsers().contains(currentUser)) {
            throw new AccessDeniedException("User does not manage this budget");
        }
    }

    private BudgetResponse toResponse(Budget budget) {
        BigDecimal currentSpending = budget.getTransactions().stream()
            .map(LedgerTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<UUID> transactionIds = budget.getTransactions().stream()
            .map(LedgerTransaction::getId)
            .toList();

        List<UUID> userIds = budget.getUsers().stream()
            .map(User::getId)
            .toList();

        return new BudgetResponse(
            budget.getId(),
            budget.getAmount(),
            currentSpending,
            userIds,
            transactionIds,
            budget.getCreatedAt(),
            budget.getUpdatedAt()
        );
    }

    private LedgerTransactionResponse toTransactionResponse(LedgerTransaction transaction) {
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
