package com.osborne.api.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.osborne.api.model.Budget;
import com.osborne.api.model.User;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    Page<Budget> findByCreatedBy(User createdBy, Pageable pageable);

}
