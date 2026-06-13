package com.osborne.api.service;

import java.util.Set;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public Page<Account> getAccountsForCurrentUser(Pageable pageable) {
	String email = SecurityContextHolder.getContext()
	    .getAuthentication()
	    .getName();
	User currentUser = userService.getUserByEmail(email);

	return accountRepository.findAccountsByUsersId(currentUser.getId(), pageable);
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
	String email = SecurityContextHolder.getContext()
	    .getAuthentication()
	    .getName();
	User currentUser = userService.getUserByEmail(email);

	Account account = Account.builder()
	    .name(request.name())
	    .type(request.type())
	    .currency(request.currency() != null ? request.currency() : "USD")
	    .initialBalance(request.initialBalance() != null ? request.initialBalance() : BigDecimal.ZERO)
	    .users(new HashSet<>(Set.of(currentUser)))
	    .build();

	return accountRepository.save(account);
    }

    @Transactional
    public Account updateAccount(UUID id, UpdateAccountRequest request) {
	String email = SecurityContextHolder.getContext()
	    .getAuthentication()
	    .getName();
	User currentUser = userService.getUserByEmail(email);

	Account account = accountRepository
	    .findById(id)
	    .orElseThrow(() ->
		new EntityNotFoundException("Account not found with id: " + id)
	    );

	if (!account.getUsers().contains(currentUser)) {
	    throw new AccessDeniedException("User does not manage this account");
	}

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
	    User targetUser = userService.getUserById(request.userId());

	    Set<User> accountUsers = account.getUsers();

	    if (!accountUsers.add(targetUser)) {
		if (accountUsers.size() <= 1) {
		    throw new IllegalStateException("Account must have at least one manager");
		}
		accountUsers.remove(targetUser);
	    }
	}

	return accountRepository.save(account);
    }

}
