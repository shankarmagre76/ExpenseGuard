package com.expenseguard.recurring.scheduler;

import com.expenseguard.recurring.service.RecurringTransactionExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job executing due active recurring transactions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RecurringTransactionScheduler {

    private final RecurringTransactionExecutionService executionService;

    /**
     * Daily scheduled job running at 1:00 AM (configurable via cron property).
     */
    @Scheduled(cron = "${expenseguard.recurring.cron:0 0 1 * * ?}")
    public void runDailyRecurringExecution() {
        log.info("Triggered daily recurring transaction execution job.");
        try {
            int executedCount = executionService.executeAllDueRecurringTransactions();
            log.info("Daily recurring execution job completed. Executed {} transactions.", executedCount);
        } catch (Exception e) {
            log.error("Error during scheduled recurring transaction execution", e);
        }
    }
}
