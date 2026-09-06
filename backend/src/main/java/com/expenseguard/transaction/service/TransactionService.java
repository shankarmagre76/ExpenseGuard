package com.expenseguard.transaction.service;

import com.expenseguard.transaction.dto.TransactionRequest;
import com.expenseguard.transaction.dto.TransactionResponse;
import com.expenseguard.transaction.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service contract for managing transaction resources with strict ownership enforcement,
 * category compatibility, and atomic account balance updates.
 */
public interface TransactionService {

    /**
     * Creates a new transaction for the currently authenticated user.
     *
     * @param request Transaction creation payload
     * @return Created TransactionResponse DTO
     */
    TransactionResponse createTransaction(TransactionRequest request);

    /**
     * Retrieves paginated transactions belonging to the currently authenticated user with optional filters.
     *
     * @param type Transaction type filter (optional)
     * @param accountId Account ID filter (optional)
     * @param categoryId Category ID filter (optional)
     * @param fromDate Start date filter (optional)
     * @param toDate End date filter (optional)
     * @param pageable Pagination and sorting options
     * @return Page of TransactionResponse DTOs
     */
    Page<TransactionResponse> getAllTransactionsForCurrentUser(
            TransactionType type,
            UUID accountId,
            UUID categoryId,
            LocalDate fromDate,
            LocalDate toDate,
            Pageable pageable
    );

    /**
     * Retrieves paginated transactions belonging to the currently authenticated user.
     *
     * @param pageable Pagination and sorting options
     * @return Page of TransactionResponse DTOs
     */
    Page<TransactionResponse> getAllTransactionsForCurrentUser(Pageable pageable);

    /**
     * Retrieves all transactions belonging to the currently authenticated user.
     *
     * @return List of TransactionResponse DTOs
     */
    List<TransactionResponse> getAllTransactionsForCurrentUser();

    /**
     * Retrieves transaction by ID ensuring ownership by the currently authenticated user.
     *
     * @param transactionId Transaction UUID
     * @return TransactionResponse DTO
     */
    TransactionResponse getTransactionById(UUID transactionId);

    /**
     * Updates an existing transaction owned by the currently authenticated user.
     *
     * @param transactionId Transaction UUID to update
     * @param request Transaction update payload
     * @return Updated TransactionResponse DTO
     */
    TransactionResponse updateTransaction(UUID transactionId, TransactionRequest request);

    /**
     * Deletes a transaction owned by the currently authenticated user and reverts balance.
     *
     * @param transactionId Transaction UUID to delete
     */
    void deleteTransaction(UUID transactionId);
}
