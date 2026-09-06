package com.expenseguard.transaction.service;

import com.expenseguard.transaction.dto.AccountRequest;
import com.expenseguard.transaction.dto.AccountResponse;
import com.expenseguard.transaction.entity.Account;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service contract for managing user account resources with strict ownership enforcement.
 */
public interface AccountService {

    /**
     * Creates a new financial account for the currently authenticated user.
     *
     * @param request Account request payload
     * @return Created AccountResponse DTO
     */
    AccountResponse createAccount(AccountRequest request);

    /**
     * Retrieves all accounts belonging to the currently authenticated user.
     *
     * @return List of AccountResponse DTOs
     */
    List<AccountResponse> getAllAccountsForCurrentUser();

    /**
     * Retrieves account by ID ensuring the account belongs to the currently authenticated user.
     *
     * @param accountId ID of account to retrieve
     * @return AccountResponse DTO
     */
    AccountResponse getAccountById(UUID accountId);

    /**
     * Updates an existing account owned by the currently authenticated user.
     *
     * @param accountId ID of account to update
     * @param request Updated account fields payload
     * @return Updated AccountResponse DTO
     */
    AccountResponse updateAccount(UUID accountId, AccountRequest request);

    /**
     * Deletes an account owned by the currently authenticated user.
     *
     * @param accountId ID of account to delete
     */
    void deleteAccount(UUID accountId);

    /**
     * Helper method to create account entity linked to current authenticated user.
     *
     * @param name Account name
     * @param currency Currency ISO code
     * @param balance Initial balance
     * @return Created Account entity
     */
    Account createAccountForCurrentUser(String name, String currency, BigDecimal balance);
}
