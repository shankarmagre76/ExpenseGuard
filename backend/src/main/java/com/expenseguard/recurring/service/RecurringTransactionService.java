package com.expenseguard.recurring.service;

import com.expenseguard.recurring.dto.RecurringTransactionRequest;
import com.expenseguard.recurring.dto.RecurringTransactionResponse;
import com.expenseguard.recurring.dto.RecurringTransactionStatusRequest;

import java.util.List;
import java.util.UUID;

/**
 * Service contract managing CRUD operations, validation, ownership security,
 * and status toggling for recurring transaction templates.
 */
public interface RecurringTransactionService {

    /**
     * Creates a new recurring transaction template for the authenticated user.
     */
    RecurringTransactionResponse createRecurringTransaction(RecurringTransactionRequest request);

    /**
     * Retrieves all recurring transactions belonging to the authenticated user.
     */
    List<RecurringTransactionResponse> getAllRecurringTransactionsForCurrentUser();

    /**
     * Retrieves a recurring transaction by ID ensuring ownership by authenticated user.
     */
    RecurringTransactionResponse getRecurringTransactionById(UUID id);

    /**
     * Updates an existing recurring transaction template owned by authenticated user.
     */
    RecurringTransactionResponse updateRecurringTransaction(UUID id, RecurringTransactionRequest request);

    /**
     * Deletes a recurring transaction template owned by authenticated user.
     */
    void deleteRecurringTransaction(UUID id);

    /**
     * Activates or deactivates a recurring transaction template.
     */
    RecurringTransactionResponse updateStatus(UUID id, RecurringTransactionStatusRequest statusRequest);
}
