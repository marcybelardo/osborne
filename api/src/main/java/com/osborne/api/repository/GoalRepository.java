package com.osborne.api.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.osborne.api.model.Goal;
import com.osborne.api.model.User;

@Repository
public interface GoalRepository extends JpaRepository<Goal, UUID> {

    Page<Goal> findByUsersContaining(User user, Pageable pageable);
}
