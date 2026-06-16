package com.osborne.api.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
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
import com.osborne.api.dto.UserSummary;
import com.osborne.api.enums.BudgetTimeframe;
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

    public record PeriodWindow(LocalDate from, LocalDate to) {}

    public static String formatPeriodLabel(BudgetTimeframe timeframe, PeriodWindow window) {
        if (timeframe == BudgetTimeframe.DAILY) {
            return window.from().format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        }
        if (timeframe == BudgetTimeframe.WEEKLY) {
            return window.from().format(DateTimeFormatter.ofPattern("MMM d"))
                + " – " + window.to().format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        }
        return window.from().format(DateTimeFormatter.ofPattern("MMM d"))
            + " – " + window.to().format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
    }

    public static PeriodWindow computeCurrentPeriod(BudgetTimeframe timeframe, LocalDate anchorDate, LocalDate endDate, LocalDate today) {
        switch (timeframe) {
            case DAILY:
                return new PeriodWindow(today, today);
            case WEEKLY: {
                if (anchorDate == null) return new PeriodWindow(today, today);
                long daysSinceAnchor = ChronoUnit.DAYS.between(anchorDate, today);
                long weekOffset = daysSinceAnchor / 7;
                LocalDate weekStart = anchorDate.plusWeeks(weekOffset);
                return new PeriodWindow(weekStart, weekStart.plusDays(6));
            }
            case MONTHLY: {
                if (anchorDate == null) return new PeriodWindow(today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()));
                int anchorDay = anchorDate.getDayOfMonth();
                int maxDayThisMonth = today.lengthOfMonth();
                int effectiveDay = Math.min(anchorDay, maxDayThisMonth);
                LocalDate candidateStart = today.withDayOfMonth(effectiveDay);
                if (candidateStart.isAfter(today)) {
                    // Current period started last month
                    LocalDate prevMonth = today.minusMonths(1);
                    int maxDayPrev = prevMonth.lengthOfMonth();
                    LocalDate periodStart = prevMonth.withDayOfMonth(Math.min(anchorDay, maxDayPrev));
                    LocalDate periodEnd = candidateStart.minusDays(1);
                    return new PeriodWindow(periodStart, periodEnd);
                }
                // Current period starts this month
                int nextMonthAnchorDay = Math.min(anchorDay, candidateStart.plusMonths(1).lengthOfMonth());
                LocalDate periodEnd = candidateStart.plusMonths(1).withDayOfMonth(nextMonthAnchorDay).minusDays(1);
                return new PeriodWindow(candidateStart, periodEnd);
            }
            case YEARLY: {
                if (anchorDate == null) return new PeriodWindow(today.withDayOfYear(1), today.withDayOfYear(today.lengthOfYear()));
                int anchorDay = anchorDate.getDayOfMonth();
                int anchorMonth = anchorDate.getMonthValue();
                int maxDayThisYear = LocalDate.of(today.getYear(), anchorMonth, 1).lengthOfMonth();
                int effectiveDay = Math.min(anchorDay, maxDayThisYear);
                LocalDate candidateStart = LocalDate.of(today.getYear(), anchorMonth, effectiveDay);
                if (candidateStart.isAfter(today)) {
                    LocalDate periodStart = candidateStart.minusYears(1);
                    int maxDayPrevYear = LocalDate.of(today.getYear(), anchorMonth, 1).lengthOfMonth();
                    LocalDate periodEnd = LocalDate.of(today.getYear(), anchorMonth, Math.min(anchorDay, maxDayPrevYear)).minusDays(1);
                    return new PeriodWindow(periodStart, periodEnd);
                }
                int maxDayNextYear = LocalDate.of(today.getYear() + 1, anchorMonth, 1).lengthOfMonth();
                LocalDate periodEnd = LocalDate.of(today.getYear() + 1, anchorMonth, Math.min(anchorDay, maxDayNextYear)).minusDays(1);
                return new PeriodWindow(candidateStart, periodEnd);
            }
            case CUSTOM:
            default:
                return new PeriodWindow(
                    anchorDate != null ? anchorDate : LocalDate.of(1900, 1, 1),
                    endDate != null ? endDate : LocalDate.of(2099, 12, 31)
                );
        }
    }

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
            .name(request.name())
            .description(request.description())
            .timeframe(request.timeframe() != null ? request.timeframe() : BudgetTimeframe.CUSTOM)
            .startDate(request.startDate())
            .endDate(request.endDate())
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

        if (request.name() != null) {
            budget.setName(request.name());
        }
        if (request.description() != null) {
            budget.setDescription(request.description());
        }
        if (request.timeframe() != null) {
            budget.setTimeframe(request.timeframe());
        }
        if (request.startDate() != null) {
            budget.setStartDate(request.startDate());
        }
        if (request.endDate() != null) {
            budget.setEndDate(request.endDate());
        }
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
        BudgetTimeframe timeframe = budget.getTimeframe() != null ? budget.getTimeframe() : BudgetTimeframe.CUSTOM;
        LocalDate today = LocalDate.now();
        PeriodWindow window = computeCurrentPeriod(timeframe, budget.getStartDate(), budget.getEndDate(), today);
        String periodLabel = formatPeriodLabel(timeframe, window);

        BigDecimal currentSpending = budget.getTransactions() != null
            ? budget.getTransactions().stream()
                .filter(t -> {
                    LocalDate txDate = t.getTransactionDate();
                    if (txDate == null) return true;
                    return !txDate.isBefore(window.from()) && !txDate.isAfter(window.to());
                })
                .filter(t -> t.getAmount().compareTo(BigDecimal.ZERO) < 0)
                .map(t -> t.getAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
            : BigDecimal.ZERO;

        List<UUID> transactionIds = budget.getTransactions() != null
            ? budget.getTransactions().stream()
                .map(LedgerTransaction::getId)
                .toList()
            : List.of();

        List<UserSummary> users = budget.getUsers().stream()
            .map(u -> new UserSummary(u.getId(), u.getDisplayName()))
            .toList();

        return new BudgetResponse(
            budget.getId(),
            budget.getName(),
            budget.getDescription(),
            timeframe,
            budget.getStartDate(),
            budget.getEndDate(),
            window.from(),
            window.to(),
            periodLabel,
            budget.getAmount(),
            currentSpending,
            users,
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
