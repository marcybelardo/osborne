package com.osborne.api.service;

import java.util.Set;
import java.util.List;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.osborne.api.dto.AccountResponse;
import com.osborne.api.dto.CreateAccountRequest;
import com.osborne.api.dto.UpdateAccountRequest;
import com.osborne.api.dto.UserSummary;
import com.osborne.api.model.Account;
import com.osborne.api.model.User;
import com.osborne.api.repository.AccountRepository;
import com.osborne.api.repository.LedgerTransactionRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final LedgerTransactionRepository ledgerTransactionRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Page<AccountResponse> getAccountsForCurrentUser(Pageable pageable) {
        User currentUser = getCurrentUser();

        // TODO: N+1 — sumAmountByAccount is called per account. Acceptable for
        // paginated results (small page sizes). Consider a custom query with a
        // subselect or DTO projection for the list endpoint to batch this.
        return accountRepository.findAccountsByUsersId(currentUser.getId(), pageable)
            .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID id) {
        Account account = accountRepository
            .findById(id)
            .orElseThrow(() ->
                new EntityNotFoundException(
                    "Account not found with id: " + id
                )
            );
        verifyAccess(account);
        return toResponse(account);
    }

    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        User currentUser = getCurrentUser();

        Account account = Account.builder()
            .name(request.name())
            .type(request.type())
            .currency(request.currency() != null ? request.currency() : "USD")
            .initialBalance(request.initialBalance() != null ? request.initialBalance() : BigDecimal.ZERO)
            .users(new HashSet<>(Set.of(currentUser)))
            .build();

        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    @Transactional
    public void deleteAccount(UUID id) {
        Account account = accountRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Account not found with id: " + id));
        verifyAccess(account);
        accountRepository.delete(account);
    }

    @Transactional(readOnly = true)
    public List<User> getAccountUsers(UUID accountId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new EntityNotFoundException("Account not found with id: " + accountId));
        verifyAccess(account);
        return List.copyOf(account.getUsers());
    }

    @Transactional
    public void addUserToAccount(UUID accountId, UUID userId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new EntityNotFoundException("Account not found with id: " + accountId));
        verifyAccess(account);

        User targetUser = userService.getUserById(userId);
        account.getUsers().add(targetUser);
        accountRepository.save(account);
    }

    @Transactional
    public void removeUserFromAccount(UUID accountId, UUID userId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new EntityNotFoundException("Account not found with id: " + accountId));
        verifyAccess(account);

        if (account.getUsers().size() <= 1) {
            throw new IllegalStateException("Account must have at least one manager");
        }

        boolean removed = account.getUsers()
            .removeIf(u -> u.getId().equals(userId));

        if (!removed) {
            throw new EntityNotFoundException("User not found on account");
        }

        accountRepository.save(account);
    }

    @Transactional
    public AccountResponse updateAccount(UUID id, UpdateAccountRequest request) {
        Account account = accountRepository
            .findById(id)
            .orElseThrow(() ->
                new EntityNotFoundException("Account not found with id: " + id)
            );
        verifyAccess(account);

        if (request.name() != null) {
            account.setName(request.name());
        }
        if (request.type() != null) {
            account.setType(request.type());
        }
        if (request.currency() != null) {
            account.setCurrency(request.currency());
        }

        Account saved = accountRepository.save(account);
        return toResponse(saved);
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();
        return userService.getUserByEmail(email);
    }

    private void verifyAccess(Account account) {
        User currentUser = getCurrentUser();
        if (!account.getUsers().contains(currentUser)) {
            throw new AccessDeniedException("User does not manage this account");
        }
    }

    private AccountResponse toResponse(Account account) {
        BigDecimal currentBalance = account.getInitialBalance()
            .add(ledgerTransactionRepository.sumAmountByAccount(account.getId()));

        List<UserSummary> users = account.getUsers().stream()
            .map(u -> new UserSummary(u.getId(), u.getDisplayName()))
            .toList();

        return new AccountResponse(
            account.getId(),
            account.getName(),
            account.getType(),
            account.getCurrency(),
            account.getInitialBalance(),
            currentBalance,
            users,
            account.getCreatedAt(),
            account.getUpdatedAt()
        );
    }

}
