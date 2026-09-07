package com.expenseguard.recurring.controller;

import com.expenseguard.auth.security.CurrentUserService;
import com.expenseguard.recurring.dto.RecurringTransactionRequest;
import com.expenseguard.recurring.dto.RecurringTransactionResponse;
import com.expenseguard.recurring.dto.RecurringTransactionStatusRequest;
import com.expenseguard.recurring.service.RecurringTransactionExecutionService;
import com.expenseguard.recurring.service.RecurringTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller exposing management and manual execution endpoints for Recurring Transactions.
 */
@RestController
@RequestMapping("/api/v1/recurring-transactions")
@RequiredArgsConstructor
public class RecurringTransactionController {

    private final RecurringTransactionService recurringTransactionService;
    private final RecurringTransactionExecutionService executionService;
    private final CurrentUserService currentUserService;

    /**
     * Create a new recurring transaction template for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<RecurringTransactionResponse> createRecurringTransaction(
            @Valid @RequestBody RecurringTransactionRequest request) {
        RecurringTransactionResponse response = recurringTransactionService.createRecurringTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all recurring transactions belonging to the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<RecurringTransactionResponse>> getAllRecurringTransactions() {
        List<RecurringTransactionResponse> responses = recurringTransactionService.getAllRecurringTransactionsForCurrentUser();
        return ResponseEntity.ok(responses);
    }

    /**
     * Get specific recurring transaction by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RecurringTransactionResponse> getRecurringTransactionById(@PathVariable("id") UUID id) {
        RecurringTransactionResponse response = recurringTransactionService.getRecurringTransactionById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Update an existing recurring transaction template.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RecurringTransactionResponse> updateRecurringTransaction(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RecurringTransactionRequest request) {
        RecurringTransactionResponse response = recurringTransactionService.updateRecurringTransaction(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a recurring transaction template.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecurringTransaction(@PathVariable("id") UUID id) {
        recurringTransactionService.deleteRecurringTransaction(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Enable or disable a recurring transaction.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<RecurringTransactionResponse> updateStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody RecurringTransactionStatusRequest statusRequest) {
        RecurringTransactionResponse response = recurringTransactionService.updateStatus(id, statusRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Manually trigger execution of due occurrences for a recurring transaction owned by user.
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<Map<String, Object>> executeManually(@PathVariable("id") UUID id) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        int count = executionService.executeRecurringTransactionForUser(id, currentUserId);
        return ResponseEntity.ok(Map.of(
                "message", "Execution triggered successfully",
                "executedCount", count
        ));
    }
}
