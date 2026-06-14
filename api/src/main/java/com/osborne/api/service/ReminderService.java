package com.osborne.api.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.osborne.api.dto.ReminderResponse;
import com.osborne.api.enums.ReminderStatus;
import com.osborne.api.model.Goal;
import com.osborne.api.model.LedgerTransaction;
import com.osborne.api.model.Reminder;
import com.osborne.api.model.User;
import com.osborne.api.repository.ReminderRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReminderService {

    private final ReminderRepository reminderRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Page<ReminderResponse> getReminders(ReminderStatus status, Pageable pageable) {
        User currentUser = getCurrentUser();
        Page<Reminder> page = status != null
            ? reminderRepository.findByUserAndStatusOrderByCreatedAtDesc(currentUser, status, pageable)
            : reminderRepository.findByUserOrderByCreatedAtDesc(currentUser, pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long getPendingCount() {
        User currentUser = getCurrentUser();
        return reminderRepository.countByUserAndStatus(currentUser, ReminderStatus.PENDING);
    }

    @Transactional
    public ReminderResponse acknowledge(UUID id) {
        Reminder reminder = reminderRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Reminder not found with id: " + id));
        reminder.setStatus(ReminderStatus.ACKNOWLEDGED);
        return toResponse(reminderRepository.save(reminder));
    }

    @Transactional
    public ReminderResponse dismiss(UUID id) {
        Reminder reminder = reminderRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Reminder not found with id: " + id));
        reminder.setStatus(ReminderStatus.DISMISSED);
        return toResponse(reminderRepository.save(reminder));
    }

    public void createBillMismatchReminder(User user, LedgerTransaction tx, BigDecimal expected) {
        Reminder reminder = Reminder.builder()
            .message(String.format("%s bill was $%.2f (expected $%.2f). Review?",
                tx.getCategory(), tx.getAmount(), expected))
            .user(user)
            .transaction(tx)
            .build();
        reminderRepository.save(reminder);
    }

    public void createGoalMilestoneReminder(User user, Goal goal, double percent) {
        Reminder reminder = Reminder.builder()
            .message(String.format("You're %.0f%% toward your %s goal!", percent, goal.getName()))
            .user(user)
            .build();
        reminderRepository.save(reminder);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.getUserByEmail(email);
    }

    private ReminderResponse toResponse(Reminder reminder) {
        return new ReminderResponse(
            reminder.getId(),
            reminder.getMessage(),
            reminder.getStatus(),
            reminder.getUser().getId(),
            reminder.getTransaction() != null ? reminder.getTransaction().getId() : null,
            reminder.getCreatedAt(),
            reminder.getUpdatedAt()
        );
    }
}
