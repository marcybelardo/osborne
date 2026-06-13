package com.osborne.api.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.osborne.api.dto.CreateBudgetRequest;
import com.osborne.api.dto.UpdateBudgetRequest;
import com.osborne.api.model.Budget;
import com.osborne.api.model.User;
import com.osborne.api.repository.BudgetRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Page<Budget> getBudgetsForCurrentUser(Pageable pageable) {
        User currentUser = getCurrentUser();
        return budgetRepository.findByCreatedBy(currentUser, pageable);
    }

    @Transactional(readOnly = true)
    public Budget getBudgetById(UUID id) {
        Budget budget = budgetRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + id));
        verifyOwnership(budget);
        return budget;
    }

    @Transactional
    public Budget createBudget(CreateBudgetRequest request) {
        User currentUser = getCurrentUser();
        Budget budget = Budget.builder()
            .amount(request.amount())
            .createdBy(currentUser)
            .build();
        return budgetRepository.save(budget);
    }

    @Transactional
    public Budget updateBudget(UUID id, UpdateBudgetRequest request) {
        Budget budget = budgetRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + id));
        verifyOwnership(budget);

        if (request.amount() != null) {
            budget.setAmount(request.amount());
        }

        return budgetRepository.save(budget);
    }

    @Transactional
    public void deleteBudget(UUID id) {
        Budget budget = budgetRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Budget not found with id: " + id));
        verifyOwnership(budget);
        budgetRepository.delete(budget);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getUserByEmail(email);
    }

    private void verifyOwnership(Budget budget) {
        User currentUser = getCurrentUser();
        if (!budget.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Not the owner of this budget");
        }
    }
}
