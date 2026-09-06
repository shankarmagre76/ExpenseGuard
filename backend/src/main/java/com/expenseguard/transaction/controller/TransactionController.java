package com.expenseguard.transaction.controller;

import com.expenseguard.transaction.dto.TransactionRequest;
import com.expenseguard.transaction.dto.TransactionResponse;
import com.expenseguard.transaction.entity.TransactionType;
import com.expenseguard.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Protected REST Controller mapping /api/v1/transactions endpoints for Transaction management.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    /**
     * Creates a new financial transaction for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.createTransaction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves paginated transactions belonging to the authenticated user with optional filtering.
     */
    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getAllTransactions(
            @RequestParam(name = "type", required = false) TransactionType type,
            @RequestParam(name = "accountId", required = false) UUID accountId,
            @RequestParam(name = "categoryId", required = false) UUID categoryId,
            @RequestParam(name = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        int cappedSize = Math.min(Math.max(size, 1), 100);
        int validPage = Math.max(page, 0);
        Pageable pageable = PageRequest.of(validPage, cappedSize, Sort.by(Sort.Direction.DESC, "transactionDate", "createdAt"));
        Page<TransactionResponse> responsePage = transactionService.getAllTransactionsForCurrentUser(
                type, accountId, categoryId, fromDate, toDate, pageable
        );
        return ResponseEntity.ok(responsePage);
    }

    /**
     * Retrieves a specific transaction by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(@PathVariable("id") UUID id) {
        TransactionResponse response = transactionService.getTransactionById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates an existing transaction owned by the authenticated user.
     */
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable("id") UUID id,
            @Valid @RequestBody TransactionRequest request
    ) {
        TransactionResponse response = transactionService.updateTransaction(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a transaction owned by the authenticated user and reverts balance.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable("id") UUID id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }
}
