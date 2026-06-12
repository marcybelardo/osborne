package com.osborne.api.service;

import java.util.List;
import java.util.Set;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.osborne.api.dto.CreateAccountRequest;
import com.osborne.api.dto.UpdateAccountRequest;
import com.osborne.api.model.Account;
import com.osborne.api.model.User;
import com.osborne.api.repository.AccountRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public List<Account> getAccountsForCurrentUser() {
	String email = SecurityContextHolder.getContext()
	    .getAuthentication()
	    .getName();
	User currentUser = userService.getUserByEmail(email);

	return accountRepository.findByOwnerId(currentUser.getId());
    }

    @Transactional(readOnly = true)
    public Account getAccountById(UUID id) {
	String email = SecurityContextHolder.getContext()
	    .getAuthentication()
	    .getName();
	User currentUser = userService.getUserByEmail(email);
	Account account = accountRepository
	    .findById(id)
	    .orElseThrow(() ->
		new EntityNotFoundException(
		    "Account not found with id: " + id
		)
	    );
	if (!account.getUsers().contains(currentUser)) {
	    throw new AccessDeniedException("User does not manage this account");
	}

	return account;
    }

    @Transactional
    public Account createAccount(CreateAccountRequest request) {
	User initialManager = userService.getUserById(request.userId());

	Account account = Account.builder()
	    .name(request.name())
	    .type(request.type())
	    .currency(request.currency() != null ? request.currency() : "USD")
	    .initialBalance(request.initialBalance() != null ? request.initialBalance() : BigDecimal.ZERO)
	    .users(new HashSet<>(Set.of(initialManager)))
	    .build();

	return accountRepository.save(account);
    }

    @Transactional
    public Account updateAccount(UUID id, UpdateAccountRequest request) {
	Account account = accountRepository
	    .findById(id)
	    .orElseThrow(() ->
		new RuntimeException("Account not found with id: " + id)
	    );

	if (request.name() != null) {
	    account.setName(request.name());
	}
	if (request.type() != null) {
	    account.setType(request.type());
	}
	if (request.currency() != null) {
	    account.setCurrency(request.currency());
	}
	if (request.userId() != null) {
	    String email = SecurityContextHolder.getContext()
		.getAuthentication()
		.getName();
	    User currentUser = userService.getUserByEmail(email);

	    Set<User> accountUsers = account.getUsers();

	    // `add` returns false if list is unchanged, in other words,
	    // if user is present we don't need to add, but must remove
	    if (!accountUsers.add(currentUser)) {
		if (accountUsers.size() <= 1) {
		    throw new IllegalStateException("Account must have at least one manager");
		}
		account.getUsers().remove(currentUser);
	    }
	}

	return accountRepository.save(account);
    }

}
