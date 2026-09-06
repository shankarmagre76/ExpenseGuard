package com.expenseguard.transaction.service;

import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.security.CurrentUserService;
import com.expenseguard.common.exception.ResourceAccessDeniedException;
import com.expenseguard.common.exception.ResourceNotFoundException;
import com.expenseguard.transaction.dto.AccountRequest;
import com.expenseguard.transaction.dto.AccountResponse;
import com.expenseguard.transaction.entity.Account;
import com.expenseguard.transaction.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation enforcing server-side ownership check for Account operations.
 */
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public AccountResponse createAccount(AccountRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        String currency = (request.getCurrency() != null && !request.getCurrency().isBlank())
                ? request.getCurrency()
                : "INR";

        Account account = Account.builder()
                .user(currentUser)
                .name(request.getName())
                .type(request.getType())
                .balance(request.getOpeningBalance())
                .currency(currency)
                .build();

        Account saved = accountRepository.save(account);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountResponse> getAllAccountsForCurrentUser() {
        UUID currentUserId = currentUserService.getCurrentUserId();
        List<Account> accounts = accountRepository.findAllByUserId(currentUserId);
        return accounts.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(UUID accountId) {
        Account account = findAndVerifyOwnership(accountId);
        return mapToResponse(account);
    }

    @Override
    @Transactional
    public AccountResponse updateAccount(UUID accountId, AccountRequest request) {
        Account account = findAndVerifyOwnership(accountId);

        account.setName(request.getName());
        account.setType(request.getType());
        account.setBalance(request.getOpeningBalance());
        if (request.getCurrency() != null && !request.getCurrency().isBlank()) {
            account.setCurrency(request.getCurrency());
        }

        Account updated = accountRepository.save(account);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void deleteAccount(UUID accountId) {
        Account account = findAndVerifyOwnership(accountId);
        accountRepository.delete(account);
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

    private Account findAndVerifyOwnership(UUID accountId) {
        UUID currentUserId = currentUserService.getCurrentUserId();

        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            throw new ResourceNotFoundException("Account not found with ID: " + accountId);
        }

        Account account = accountOpt.get();

        if (!account.getUser().getId().equals(currentUserId)) {
            throw new ResourceAccessDeniedException("Access denied: You do not have permission to access this account");
        }

        return account;
    }

    private AccountResponse mapToResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .userId(account.getUser().getId())
                .name(account.getName())
                .type(account.getType())
                .balance(account.getBalance())
                .currency(account.getCurrency())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
