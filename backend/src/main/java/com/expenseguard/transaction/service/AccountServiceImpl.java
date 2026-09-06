package com.expenseguard.transaction.service;

import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.security.CurrentUserService;
import com.expenseguard.common.exception.ResourceAccessDeniedException;
import com.expenseguard.common.exception.ResourceNotFoundException;
import com.expenseguard.transaction.dto.AccountResponse;
import com.expenseguard.transaction.entity.Account;
import com.expenseguard.transaction.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation enforcing server-side ownership check for Account operations.
 */
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID accountId) {
        UUID currentUserId = currentUserService.getCurrentUserId();

        // 1. Check if resource exists in database
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            throw new ResourceNotFoundException("Account not found with ID: " + accountId);
        }

        Account account = accountOpt.get();

        // 2. Enforce strict server-side ownership check against JWT identity
        if (!account.getUser().getId().equals(currentUserId)) {
            throw new ResourceAccessDeniedException("Access denied: You do not have permission to access this account");
        }

        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUser().getId())
                .name(account.getName())
                .currency(account.getCurrency())
                .balance(account.getBalance())
                .build();
    }

    @Override
    @Transactional
    public Account createAccountForCurrentUser(String name, String currency, BigDecimal balance) {
        User currentUser = currentUserService.getCurrentUser();
        Account account = Account.builder()
                .user(currentUser)
                .name(name)
                .currency(currency)
                .balance(balance)
                .build();
        return accountRepository.save(account);
    }
}
