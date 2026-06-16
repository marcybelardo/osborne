package com.osborne.api.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.osborne.api.enums.BudgetTimeframe;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "budgets")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget extends BaseEntity {

    @NotBlank(message = "Budget name is required")
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    @NotNull
    @Builder.Default
    private BudgetTimeframe timeframe = BudgetTimeframe.CUSTOM;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToMany
    @JoinTable(
	name = "budget_users",
	joinColumns = @JoinColumn(name = "budget_id"),
	inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    @Builder.Default
    @JsonIgnore
    private Set<User> users = new HashSet<>();

    @OneToMany
    @JoinTable(
	name = "budget_transactions",
	joinColumns = @JoinColumn(name = "budget_id"),
	inverseJoinColumns = @JoinColumn(name = "transaction_id")
    )
    @Builder.Default
    private List<LedgerTransaction> transactions = new ArrayList<>();

}
