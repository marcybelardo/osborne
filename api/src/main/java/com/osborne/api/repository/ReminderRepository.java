package com.osborne.api.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.osborne.api.enums.ReminderStatus;
import com.osborne.api.model.Reminder;
import com.osborne.api.model.User;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, UUID> {

    Page<Reminder> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Page<Reminder> findByUserAndStatusOrderByCreatedAtDesc(User user, ReminderStatus status, Pageable pageable);

    long countByUserAndStatus(User user, ReminderStatus status);
}
