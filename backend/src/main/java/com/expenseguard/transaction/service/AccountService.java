package com.expenseguard.transaction.service;

import com.expenseguard.transaction.dto.AccountResponse;
import com.expenseguard.transaction.entity.Account;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service contract for managing user account resources with strict ownership enforcement.
 */
public interface AccountService {

    /**
     * Retrieves account by ID ensuring the account belongs to the currently authenticated user.
     *
     * @param accountId ID of account to retrieve
     * @return AccountResponse DTO
     */
    AccountResponse getAccountById(UUID accountId);

    /**
     * Helper method to create account linked to current authenticated user.
     *
     * @param name Account name
     * @param currency Currency ISO code
     * @param balance Initial balance
     * @return Created Account entity
     */
    Account createAccountForCurrentUser(String name, String currency, BigDecimal balance);
}
