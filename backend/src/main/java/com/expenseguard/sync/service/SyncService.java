package com.expenseguard.sync.service;

import com.expenseguard.sync.dto.*;

/**
 * Service interface coordinating offline synchronization requests, idempotency,
 * optimistic conflict detection, and atomic balance updates.
 */
public interface SyncService {

    SyncTransactionResponse processSyncTransaction(SyncTransactionRequest request);

    BatchSyncResponse processBatchSyncTransactions(BatchSyncRequest request);

    SyncStatusResponse getSyncStatus(String clientOperationId);
}
