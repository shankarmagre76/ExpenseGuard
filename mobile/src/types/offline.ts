/**
 * Mobile offline synchronization and local persistence types matching Spring Boot backend DTOs.
 */

export type SyncOperationType = 'CREATE' | 'UPDATE' | 'DELETE';
export type SyncEntityType = 'TRANSACTION';
export type SyncStatus = 'PENDING' | 'SYNCING' | 'SYNCED' | 'FAILED' | 'CONFLICT';

export interface OfflineTransactionItem {
  clientOperationId: string;
  operationType: SyncOperationType;
  entityType?: SyncEntityType;
  transactionId?: string; // Assigned server UUID once created/synced or for UPDATE/DELETE
  accountId: string;
  categoryId: string;
  categoryName?: string;
  type: 'EXPENSE' | 'INCOME';
  amount: number;
  transactionDate: string; // YYYY-MM-DD
  description?: string;
  version?: number; // Required for UPDATE optimistic locking check

  // Local metadata
  userId: string; // Authenticated user ID for ownership isolation
  status: SyncStatus;
  retryCount: number;
  lastAttemptAt?: string; // ISO string timestamp
  errorCode?: string;
  errorMessage?: string;
  createdAt: string; // ISO string timestamp
}

export interface SyncTransactionRequest {
  clientOperationId: string;
  operationType: SyncOperationType;
  entityType?: SyncEntityType;
  transactionId?: string;
  accountId?: string;
  categoryId?: string;
  type?: 'EXPENSE' | 'INCOME';
  amount?: number;
  transactionDate?: string;
  description?: string;
  version?: number;
}

export interface SyncTransactionResponse {
  clientOperationId: string;
  status: 'PROCESSED' | 'CONFLICT' | 'FAILED' | 'PENDING';
  transactionId?: string;
  serverVersion?: number;
  errorCode?: string;
  message?: string;
}

export interface BatchSyncRequest {
  operations: SyncTransactionRequest[];
}

export interface BatchSyncResponse {
  results: SyncTransactionResponse[];
}

export interface SyncStatusResponse {
  clientOperationId: string;
  status: 'PENDING' | 'PROCESSED' | 'CONFLICT' | 'FAILED';
  transactionId?: string;
  errorCode?: string;
  errorMessage?: string;
  createdAt?: string;
  processedAt?: string;
}
