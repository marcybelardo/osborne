package com.osborne.api.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
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
