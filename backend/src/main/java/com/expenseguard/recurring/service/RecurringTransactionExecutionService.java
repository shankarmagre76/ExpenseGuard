package com.expenseguard.recurring.service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service interface responsible for executing due recurring transactions,
 * advancing run dates, enforcing catch-up limits, and ensuring idempotency.
 */
public interface RecurringTransactionExecutionService {

    /**
     * Finds and executes all due recurring transactions for the current date.
     *
     * @return Number of new transactions generated.
     */
    int executeAllDueRecurringTransactions();

    /**
     * Finds and executes all due recurring transactions for a specified target date.
     *
     * @param date Target date to compare nextRunDate against
     * @return Number of new transactions generated.
     */
    int executeAllDueRecurringTransactionsForDate(LocalDate date);

    /**
     * Manually triggers execution for a specific user's recurring transaction.
     *
     * @param recurringTransactionId Recurring transaction UUID
     * @param userId User UUID for security verification
     * @return Number of transactions generated during this execution.
     */
    int executeRecurringTransactionForUser(UUID recurringTransactionId, UUID userId);
}
