package com.expenseguard.recurring.service;

import com.expenseguard.auth.entity.User;
import com.expenseguard.common.exception.ResourceAccessDeniedException;
import com.expenseguard.common.exception.ResourceNotFoundException;
import com.expenseguard.recurring.entity.RecurrenceFrequency;
import com.expenseguard.recurring.entity.RecurringTransaction;
import com.expenseguard.recurring.repository.RecurringTransactionRepository;
import com.expenseguard.transaction.dto.TransactionRequest;
import com.expenseguard.transaction.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service implementation managing recurring transaction occurrence execution,
 * idempotency deduplication, frequency advancement, and catch-up safety limits.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecurringTransactionExecutionServiceImpl implements RecurringTransactionExecutionService {

    public static final int MAX_CATCH_UP_LIMIT = 12;

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final TransactionService transactionService;

    @Override
    @Transactional
    public int executeAllDueRecurringTransactions() {
        return executeAllDueRecurringTransactionsForDate(LocalDate.now());
    }

    @Override
    @Transactional
    public int executeAllDueRecurringTransactionsForDate(LocalDate targetDate) {
        log.info("Starting recurring transaction execution for target date: {}", targetDate);
        List<RecurringTransaction> dueItems = recurringTransactionRepository
                .findDueRecurringTransactionsWithLock(targetDate);

        int totalGenerated = 0;
        for (RecurringTransaction item : dueItems) {
            totalGenerated += processRecurringTransactionOccurrences(item, targetDate);
        }
        log.info("Completed recurring transaction execution. Total transactions generated: {}", totalGenerated);
        return totalGenerated;
    }

    @Override
    @Transactional
    public int executeRecurringTransactionForUser(UUID recurringTransactionId, UUID userId) {
        LocalDate today = LocalDate.now();

        // 1. Verify existence and ownership
        RecurringTransaction item = recurringTransactionRepository.findById(recurringTransactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Recurring transaction not found with ID: " + recurringTransactionId));

        if (!item.getUser().getId().equals(userId)) {
            throw new ResourceAccessDeniedException("Access denied: You do not have permission to execute this recurring transaction");
        }

        if (!item.isActive()) {
            log.info("Recurring transaction ID {} is inactive. Skipping execution.", recurringTransactionId);
            return 0;
        }

        // 2. Fetch locked entity if due
        Optional<RecurringTransaction> lockedOpt = recurringTransactionRepository
                .findDueByIdWithLock(recurringTransactionId, today);

        if (lockedOpt.isEmpty()) {
            log.info("Recurring transaction ID {} is not due for date {}.", recurringTransactionId, today);
            return 0;
        }

        return processRecurringTransactionOccurrences(lockedOpt.get(), today);
    }

    private int processRecurringTransactionOccurrences(RecurringTransaction item, LocalDate targetDate) {
        int occurrencesProcessed = 0;
        User owner = item.getUser();
        org.springframework.security.core.Authentication previousAuth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        try {
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken auth =
                    new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                            owner, null, java.util.Collections.emptyList()
                    );
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

            while (item.isActive()
                    && !item.getNextRunDate().isAfter(targetDate)
                    && (item.getEndDate() == null || !item.getNextRunDate().isAfter(item.getEndDate()))
                    && occurrencesProcessed < MAX_CATCH_UP_LIMIT) {

                LocalDate scheduledDate = item.getNextRunDate();
                String clientOpId = "recurring:" + item.getId() + ":" + scheduledDate;

                TransactionRequest txRequest = TransactionRequest.builder()
                        .accountId(item.getAccount().getId())
                        .categoryId(item.getCategory().getId())
                        .type(item.getType())
                        .amount(item.getAmount())
                        .transactionDate(scheduledDate)
                        .description(item.getDescription())
                        .clientOperationId(clientOpId)
                        .build();

                // Create actual transaction via TransactionService (reuses balance & idempotency logic)
                transactionService.createTransaction(txRequest);

                // Advance next run date according to frequency
                LocalDate nextRun = advanceDate(scheduledDate, item.getFrequency());
                item.setNextRunDate(nextRun);

                // Deactivate if nextRun date passes endDate
                if (item.getEndDate() != null && nextRun.isAfter(item.getEndDate())) {
                    item.setActive(false);
                }

                occurrencesProcessed++;
            }
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(previousAuth);
        }

        recurringTransactionRepository.save(item);
        log.debug("Processed {} occurrences for recurring transaction ID: {}", occurrencesProcessed, item.getId());
        return occurrencesProcessed;
    }

    private LocalDate advanceDate(LocalDate current, RecurrenceFrequency frequency) {
        return switch (frequency) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
            case YEARLY -> current.plusYears(1);
        };
    }
}
