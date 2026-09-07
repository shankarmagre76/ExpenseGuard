package com.expenseguard.sync.controller;

import com.expenseguard.sync.dto.*;
import com.expenseguard.sync.entity.SyncOperationStatus;
import com.expenseguard.sync.service.SyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing mobile offline synchronization endpoints for single,
 * batch, and status lookup operations.
 */
@RestController
@RequestMapping("/api/v1/sync")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;

    @PostMapping("/transactions")
    public ResponseEntity<SyncTransactionResponse> processSyncTransaction(
            @Valid @RequestBody SyncTransactionRequest request) {
        SyncTransactionResponse response = syncService.processSyncTransaction(request);

        if (response.getStatus() == SyncOperationStatus.CONFLICT) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transactions/batch")
    public ResponseEntity<BatchSyncResponse> processBatchSyncTransactions(
            @Valid @RequestBody BatchSyncRequest request) {
        BatchSyncResponse response = syncService.processBatchSyncTransactions(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{clientOperationId}")
    public ResponseEntity<SyncStatusResponse> getSyncStatus(
            @PathVariable String clientOperationId) {
        SyncStatusResponse response = syncService.getSyncStatus(clientOperationId);
        return ResponseEntity.ok(response);
    }
}
