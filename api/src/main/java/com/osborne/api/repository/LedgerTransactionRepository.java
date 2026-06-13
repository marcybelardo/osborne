package com.osborne.api.repository;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.osborne.api.model.Account;
import com.osborne.api.model.LedgerTransaction;

@Repository
public interface LedgerTransactionRepository extends JpaRepository<LedgerTransaction, UUID> {

    Page<LedgerTransaction> findByAccount(Account account, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM LedgerTransaction t WHERE t.account.id = :accountId")
    BigDecimal sumAmountByAccount(@Param("accountId") UUID accountId);

    Page<LedgerTransaction> findByBudgetsId(UUID budgetId, Pageable pageable);

}
