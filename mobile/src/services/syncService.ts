import { processSyncTransaction } from '../api/endpoints/syncApi';
import {
  getPendingSyncQueue,
  updateOfflineTransactionStatus,
  getOfflineTransactions,
} from '../storage/offlineStorage';
import {
  SyncTransactionRequest,
  SyncTransactionResponse,
} from '../types/offline';
import { AppError } from '../types/api';

export interface SyncResult {
  syncedCount: number;
  failedCount: number;
  conflictCount: number;
  pendingCount: number;
  isSyncing: boolean;
}

class SyncService {
  private isSyncingLock: boolean = false;

  public isSyncLocked(): boolean {
    return this.isSyncingLock;
  }

  public async syncPendingOperations(userId: string): Promise<SyncResult> {
    if (!userId) {
      return { syncedCount: 0, failedCount: 0, conflictCount: 0, pendingCount: 0, isSyncing: false };
    }

    if (this.isSyncingLock) {
      const currentQueue = await getPendingSyncQueue(userId);
      return {
        syncedCount: 0,
        failedCount: 0,
        conflictCount: 0,
        pendingCount: currentQueue.length,
        isSyncing: true,
      };
    }

    this.isSyncingLock = true;
    let syncedCount = 0;
    let failedCount = 0;
    let conflictCount = 0;

    try {
      const pendingQueue = await getPendingSyncQueue(userId);
      // Sort chronologically (oldest pending operation first)
      pendingQueue.sort((a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime());

      for (const item of pendingQueue) {
        // Update status to SYNCING before calling API
        await updateOfflineTransactionStatus(item.clientOperationId, {
          status: 'SYNCING',
          lastAttemptAt: new Date().toISOString(),
        });

        const syncRequest: SyncTransactionRequest = {
          clientOperationId: item.clientOperationId,
          operationType: item.operationType,
          entityType: item.entityType || 'TRANSACTION',
          transactionId: item.transactionId,
          accountId: item.accountId,
          categoryId: item.categoryId,
          type: item.type,
          amount: item.amount,
          transactionDate: item.transactionDate,
          description: item.description,
          version: item.version,
        };

        try {
          const response: SyncTransactionResponse = await processSyncTransaction(syncRequest);

          if (response.status === 'PROCESSED') {
            await updateOfflineTransactionStatus(item.clientOperationId, {
              status: 'SYNCED',
              transactionId: response.transactionId || item.transactionId,
              version: response.serverVersion !== undefined ? response.serverVersion : item.version,
              errorCode: undefined,
              errorMessage: undefined,
            });
            syncedCount++;
          } else if (response.status === 'CONFLICT') {
            await updateOfflineTransactionStatus(item.clientOperationId, {
              status: 'CONFLICT',
              errorCode: response.errorCode || 'CONFLICT',
              errorMessage: response.message || 'Conflict detected on server',
            });
            conflictCount++;
          } else {
            await updateOfflineTransactionStatus(item.clientOperationId, {
              status: 'FAILED',
              retryCount: item.retryCount + 1,
              errorCode: response.errorCode || 'SYNC_FAILED',
              errorMessage: response.message || 'Synchronization failed',
            });
            failedCount++;
          }
        } catch (err: any) {
          const appErr = err as AppError;
          const statusCode = appErr.status;

          if (statusCode === 409) {
            // 409 Conflict
            await updateOfflineTransactionStatus(item.clientOperationId, {
              status: 'CONFLICT',
              errorCode: appErr.code || 'CONFLICT',
              errorMessage: appErr.message || 'Conflict detected during sync',
            });
            conflictCount++;
          } else if (statusCode === 401) {
            // 401 Unauthorized - Revert to PENDING & halt queue processing until re-auth
            await updateOfflineTransactionStatus(item.clientOperationId, {
              status: 'PENDING',
            });
            break;
          } else if (statusCode === 403 || statusCode === 400) {
            // Permanent failure (Forbidden or Bad Request validation error)
            await updateOfflineTransactionStatus(item.clientOperationId, {
              status: 'FAILED',
              retryCount: item.retryCount + 1,
              errorCode: appErr.code || `HTTP_${statusCode}`,
              errorMessage: appErr.message || 'Permanent sync error',
            });
            failedCount++;
          } else {
            // Temporary network error / timeout / 5xx server error
            await updateOfflineTransactionStatus(item.clientOperationId, {
              status: 'PENDING',
              retryCount: item.retryCount + 1,
              errorCode: appErr.code || 'NETWORK_ERROR',
              errorMessage: appErr.message || 'Network connection failed',
            });
            // Stop remaining queue execution if network is unreachable
            break;
          }
        }
      }

      const remainingQueue = await getPendingSyncQueue(userId);
      return {
        syncedCount,
        failedCount,
        conflictCount,
        pendingCount: remainingQueue.length,
        isSyncing: false,
      };
    } finally {
      this.isSyncingLock = false;
    }
  }

  public async getSyncSummary(userId: string) {
    if (!userId) {
      return { pendingCount: 0, syncedCount: 0, failedCount: 0, conflictCount: 0 };
    }
    const allUserItems = await getOfflineTransactions(userId);
    const pendingCount = allUserItems.filter((i) => i.status === 'PENDING' || i.status === 'SYNCING').length;
    const syncedCount = allUserItems.filter((i) => i.status === 'SYNCED').length;
    const failedCount = allUserItems.filter((i) => i.status === 'FAILED').length;
    const conflictCount = allUserItems.filter((i) => i.status === 'CONFLICT').length;
    return { pendingCount, syncedCount, failedCount, conflictCount };
  }
}

export const syncService = new SyncService();
