package com.osborne.api.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.osborne.api.dto.CreateGoalRequest;
import com.osborne.api.dto.GoalResponse;
import com.osborne.api.dto.UpdateGoalRequest;
import com.osborne.api.dto.UserSummary;
import com.osborne.api.model.Goal;
import com.osborne.api.model.LedgerTransaction;
import com.osborne.api.model.User;
import com.osborne.api.repository.GoalRepository;
import com.osborne.api.repository.LedgerTransactionRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepository goalRepository;
    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Page<GoalResponse> getGoalsForCurrentUser(Pageable pageable) {
        User currentUser = getCurrentUser();
        return goalRepository.findByUsersContaining(currentUser, pageable)
            .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public GoalResponse getGoalById(UUID id) {
        Goal goal = goalRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Goal not found with id: " + id));
        verifyAccess(goal);
        return toResponse(goal);
    }

    @Transactional
    public GoalResponse createGoal(CreateGoalRequest request) {
        User currentUser = getCurrentUser();
        Goal goal = Goal.builder()
            .name(request.name())
            .targetAmount(request.targetAmount())
            .targetDate(request.targetDate())
            .users(new HashSet<>(Set.of(currentUser)))
            .build();
        Goal saved = goalRepository.save(goal);
        return toResponse(saved);
    }

    @Transactional
    public GoalResponse updateGoal(UUID id, UpdateGoalRequest request) {
        Goal goal = goalRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Goal not found with id: " + id));
        verifyAccess(goal);

        if (request.name() != null) {
            goal.setName(request.name());
        }
        if (request.targetAmount() != null) {
            goal.setTargetAmount(request.targetAmount());
        }
        if (request.targetDate() != null) {
            goal.setTargetDate(request.targetDate());
        }

        Goal saved = goalRepository.save(goal);
        return toResponse(saved);
    }

    @Transactional
    public void deleteGoal(UUID id) {
        Goal goal = goalRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Goal not found with id: " + id));
        verifyAccess(goal);
        goalRepository.delete(goal);
    }

    @Transactional(readOnly = true)
    public List<User> getGoalUsers(UUID goalId) {
        Goal goal = goalRepository.findById(goalId)
            .orElseThrow(() -> new EntityNotFoundException("Goal not found with id: " + goalId));
        verifyAccess(goal);
        return List.copyOf(goal.getUsers());
    }

    @Transactional
    public void addUserToGoal(UUID goalId, UUID userId) {
        Goal goal = goalRepository.findById(goalId)
            .orElseThrow(() -> new EntityNotFoundException("Goal not found with id: " + goalId));
        verifyAccess(goal);

        User targetUser = userService.getUserById(userId);
        goal.getUsers().add(targetUser);
        goalRepository.save(goal);
    }

    @Transactional
    public void removeUserFromGoal(UUID goalId, UUID userId) {
        Goal goal = goalRepository.findById(goalId)
            .orElseThrow(() -> new EntityNotFoundException("Goal not found with id: " + goalId));
        verifyAccess(goal);

        if (goal.getUsers().size() <= 1) {
            throw new IllegalStateException("Goal must have at least one user");
        }

        boolean removed = goal.getUsers()
            .removeIf(u -> u.getId().equals(userId));

        if (!removed) {
            throw new EntityNotFoundException("User not found on goal");
        }

        goalRepository.save(goal);
    }

    @Transactional(readOnly = true)
    public List<LedgerTransaction> getGoalTransactions(UUID goalId) {
        Goal goal = goalRepository.findById(goalId)
            .orElseThrow(() -> new EntityNotFoundException("Goal not found with id: " + goalId));
        verifyAccess(goal);
        return List.copyOf(goal.getTransactions());
    }

    @Transactional
    public void addTransactionToGoal(UUID goalId, UUID transactionId) {
        Goal goal = goalRepository.findById(goalId)
            .orElseThrow(() -> new EntityNotFoundException("Goal not found with id: " + goalId));
        verifyAccess(goal);

        LedgerTransaction transaction = ledgerTransactionRepository.findById(transactionId)
            .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + transactionId));

        goal.getTransactions().add(transaction);
        goalRepository.save(goal);
    }

    @Transactional
    public void removeTransactionFromGoal(UUID goalId, UUID transactionId) {
        Goal goal = goalRepository.findById(goalId)
            .orElseThrow(() -> new EntityNotFoundException("Goal not found with id: " + goalId));
        verifyAccess(goal);

        boolean removed = goal.getTransactions()
            .removeIf(t -> t.getId().equals(transactionId));

        if (!removed) {
            throw new EntityNotFoundException("Transaction not found in goal");
        }

        goalRepository.save(goal);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getUserByEmail(email);
    }

    private void verifyAccess(Goal goal) {
        User currentUser = getCurrentUser();
        if (!goal.getUsers().contains(currentUser)) {
            throw new AccessDeniedException("User does not manage this goal");
        }
    }

    private GoalResponse toResponse(Goal goal) {
        BigDecimal currentAmount = goal.getTransactions().stream()
            .map(LedgerTransaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        double progressPercent = 0.0;
        if (goal.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            progressPercent = currentAmount
                .divide(goal.getTargetAmount(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
        }

        List<UserSummary> users = goal.getUsers().stream()
            .map(u -> new UserSummary(u.getId(), u.getDisplayName()))
            .toList();

        List<UUID> transactionIds = goal.getTransactions().stream()
            .map(LedgerTransaction::getId)
            .toList();

        return new GoalResponse(
            goal.getId(),
            goal.getName(),
            goal.getTargetAmount(),
            currentAmount,
            goal.getTargetDate(),
            progressPercent,
            users,
            transactionIds,
            goal.getCreatedAt(),
            goal.getUpdatedAt()
        );
    }
}
