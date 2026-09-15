import {
  saveOfflineTransaction,
  getOfflineTransactions,
  getPendingSyncQueue,
  updateOfflineTransactionStatus,
  clearAllOfflineStorage,
  clearUserOfflineData,
} from '../src/storage/offlineStorage';
import {
  buildCanonicalPayloadString,
  calculatePayloadHash,
  stripTrailingZeros,
} from '../src/utils/payloadHash';
import { networkService } from '../src/services/networkService';
import { syncService } from '../src/services/syncService';
import { processSyncTransaction } from '../src/api/endpoints/syncApi';
import { OfflineTransactionItem, SyncTransactionRequest } from '../src/types/offline';

// Mock sync API
jest.mock('../src/api/endpoints/syncApi', () => ({
  processSyncTransaction: jest.fn(),
}));

const mockedProcessSyncTransaction = processSyncTransaction as jest.MockedFunction<typeof processSyncTransaction>;

describe('Phase 5.4 — Offline Synchronization & Local Data Capture', () => {
  beforeEach(async () => {
    await clearAllOfflineStorage();
    jest.clearAllMocks();
    networkService.resetMockStatus();
  });

  describe('1. Storage Abstraction & User Isolation', () => {
    it('saves and retrieves offline transaction records for a specific user', async () => {
      const item: OfflineTransactionItem = {
        clientOperationId: 'client-op-001',
        operationType: 'CREATE',
        accountId: 'acc-111',
        categoryId: 'cat-222',
        type: 'EXPENSE',
        amount: 500,
        transactionDate: '2026-09-15',
        userId: 'user-A',
        status: 'PENDING',
        retryCount: 0,
        createdAt: new Date().toISOString(),
      };

      await saveOfflineTransaction(item);
      const itemsUserA = await getOfflineTransactions('user-A');
      expect(itemsUserA.length).toBe(1);
      expect(itemsUserA[0].clientOperationId).toBe('client-op-001');
      expect(itemsUserA[0].status).toBe('PENDING');

      // User B should receive 0 records (User Isolation)
      const itemsUserB = await getOfflineTransactions('user-B');
      expect(itemsUserB.length).toBe(0);
    });

    it('retrieves only pending items in sync queue', async () => {
      const item1: OfflineTransactionItem = {
        clientOperationId: 'op-pending',
        operationType: 'CREATE',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 100,
        transactionDate: '2026-09-15',
        userId: 'user-A',
        status: 'PENDING',
        retryCount: 0,
        createdAt: '2026-09-15T10:00:00Z',
      };

      const item2: OfflineTransactionItem = {
        clientOperationId: 'op-synced',
        operationType: 'CREATE',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 200,
        transactionDate: '2026-09-15',
        userId: 'user-A',
        status: 'SYNCED',
        retryCount: 0,
        createdAt: '2026-09-15T11:00:00Z',
      };

      await saveOfflineTransaction(item1);
      await saveOfflineTransaction(item2);

      const queue = await getPendingSyncQueue('user-A');
      expect(queue.length).toBe(1);
      expect(queue[0].clientOperationId).toBe('op-pending');
    });

    it('updates offline transaction status and server details', async () => {
      const item: OfflineTransactionItem = {
        clientOperationId: 'op-to-update',
        operationType: 'CREATE',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 300,
        transactionDate: '2026-09-15',
        userId: 'user-A',
        status: 'PENDING',
        retryCount: 0,
        createdAt: new Date().toISOString(),
      };

      await saveOfflineTransaction(item);
      await updateOfflineTransactionStatus('op-to-update', {
        status: 'SYNCED',
        transactionId: 'server-uuid-999',
        version: 1,
      });

      const updated = await getOfflineTransactions('user-A');
      expect(updated[0].status).toBe('SYNCED');
      expect(updated[0].transactionId).toBe('server-uuid-999');
      expect(updated[0].version).toBe(1);
    });

    it('deletes user offline data safely', async () => {
      await saveOfflineTransaction({
        clientOperationId: 'op-user-a',
        operationType: 'CREATE',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 100,
        transactionDate: '2026-09-15',
        userId: 'user-A',
        status: 'PENDING',
        retryCount: 0,
        createdAt: new Date().toISOString(),
      });

      await saveOfflineTransaction({
        clientOperationId: 'op-user-b',
        operationType: 'CREATE',
        accountId: 'acc-2',
        categoryId: 'cat-2',
        type: 'INCOME',
        amount: 500,
        transactionDate: '2026-09-15',
        userId: 'user-B',
        status: 'PENDING',
        retryCount: 0,
        createdAt: new Date().toISOString(),
      });

      await clearUserOfflineData('user-A');

      expect((await getOfflineTransactions('user-A')).length).toBe(0);
      expect((await getOfflineTransactions('user-B')).length).toBe(1);
    });
  });

  describe('2. SHA-256 Payload Hashing & Canonicalization', () => {
    it('strips trailing zeros from amounts correctly', () => {
      expect(stripTrailingZeros(500.00)).toBe('500');
      expect(stripTrailingZeros('500.50')).toBe('500.5');
      expect(stripTrailingZeros('100')).toBe('100');
      expect(stripTrailingZeros(null)).toBe('');
    });

    it('builds exact canonical payload string matching backend format', () => {
      const request: SyncTransactionRequest = {
        clientOperationId: 'op-001',
        operationType: 'CREATE',
        entityType: 'TRANSACTION',
        transactionId: 'tx-123',
        accountId: 'acc-456',
        categoryId: 'cat-789',
        type: 'EXPENSE',
        amount: 500.00,
        transactionDate: '2026-09-15',
        description: '  Lunch with team  ',
        version: 0,
      };

      const canonical = buildCanonicalPayloadString(request);
      expect(canonical).toBe('CREATE|TRANSACTION|tx-123|acc-456|cat-789|EXPENSE|500|2026-09-15|Lunch with team|0');
    });

    it('generates a 64-character hex SHA-256 hash deterministically', () => {
      const request: SyncTransactionRequest = {
        clientOperationId: 'op-001',
        operationType: 'CREATE',
        entityType: 'TRANSACTION',
        accountId: 'acc-456',
        categoryId: 'cat-789',
        type: 'EXPENSE',
        amount: 500,
        transactionDate: '2026-09-15',
      };

      const hash1 = calculatePayloadHash(request);
      const hash2 = calculatePayloadHash(request);

      expect(hash1).toHaveLength(64);
      expect(hash1).toBe(hash2);
    });
  });

  describe('3. Network Connectivity Service', () => {
    it('detects online and offline transitions and notifies subscribers', async () => {
      let currentOnlineState = true;
      const listener = jest.fn((isOnline: boolean) => {
        currentOnlineState = isOnline;
      });

      const unsubscribe = networkService.subscribe(listener);

      networkService.setMockStatus(false);
      expect(listener).toHaveBeenCalledWith(false);
      expect(currentOnlineState).toBe(false);

      networkService.setMockStatus(true);
      expect(listener).toHaveBeenCalledWith(true);
      expect(currentOnlineState).toBe(true);

      unsubscribe();
    });
  });

  describe('4. Sync Engine & Execution Lock', () => {
    it('synchronizes pending operations successfully and updates status to SYNCED', async () => {
      await saveOfflineTransaction({
        clientOperationId: 'client-op-100',
        operationType: 'CREATE',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 250,
        transactionDate: '2026-09-15',
        userId: 'user-sync',
        status: 'PENDING',
        retryCount: 0,
        createdAt: new Date().toISOString(),
      });

      mockedProcessSyncTransaction.mockResolvedValueOnce({
        clientOperationId: 'client-op-100',
        status: 'PROCESSED',
        transactionId: 'server-tx-uuid-100',
        serverVersion: 0,
        message: 'Transaction synchronized successfully',
      });

      const result = await syncService.syncPendingOperations('user-sync');

      expect(result.syncedCount).toBe(1);
      expect(result.pendingCount).toBe(0);

      const items = await getOfflineTransactions('user-sync');
      expect(items[0].status).toBe('SYNCED');
      expect(items[0].transactionId).toBe('server-tx-uuid-100');
    });

    it('prevents parallel duplicate sync executions using single-lock', async () => {
      await saveOfflineTransaction({
        clientOperationId: 'client-op-lock',
        operationType: 'CREATE',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 100,
        transactionDate: '2026-09-15',
        userId: 'user-lock',
        status: 'PENDING',
        retryCount: 0,
        createdAt: new Date().toISOString(),
      });

      // Mock slow processSyncTransaction
      mockedProcessSyncTransaction.mockImplementation(
        () => new Promise((resolve) => setTimeout(() => resolve({
          clientOperationId: 'client-op-lock',
          status: 'PROCESSED',
          transactionId: 'server-tx-lock',
        }), 100))
      );

      const syncPromise1 = syncService.syncPendingOperations('user-lock');
      const syncPromise2 = syncService.syncPendingOperations('user-lock');

      const [res1, res2] = await Promise.all([syncPromise1, syncPromise2]);

      // First execution acquires lock and processes, second execution is rejected by lock check (isSyncing: true)
      expect(res1.isSyncing).toBe(false);
      expect(res2.isSyncing).toBe(true);
      expect(mockedProcessSyncTransaction).toHaveBeenCalledTimes(1);
    });

    it('handles temporary network failures by keeping status PENDING for retry', async () => {
      await saveOfflineTransaction({
        clientOperationId: 'client-op-neterr',
        operationType: 'CREATE',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 400,
        transactionDate: '2026-09-15',
        userId: 'user-retry',
        status: 'PENDING',
        retryCount: 0,
        createdAt: new Date().toISOString(),
      });

      mockedProcessSyncTransaction.mockRejectedValueOnce({
        status: 0,
        code: 'NETWORK_ERROR',
        message: 'Network request failed',
      });

      const result = await syncService.syncPendingOperations('user-retry');

      expect(result.syncedCount).toBe(0);
      expect(result.pendingCount).toBe(1);

      const items = await getOfflineTransactions('user-retry');
      expect(items[0].status).toBe('PENDING');
      expect(items[0].retryCount).toBe(1);
    });

    it('halts sync loop on 401 Unauthorized without clearing pending queue', async () => {
      await saveOfflineTransaction({
        clientOperationId: 'client-op-401',
        operationType: 'CREATE',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 500,
        transactionDate: '2026-09-15',
        userId: 'user-401',
        status: 'PENDING',
        retryCount: 0,
        createdAt: new Date().toISOString(),
      });

      mockedProcessSyncTransaction.mockRejectedValueOnce({
        status: 401,
        code: 'UNAUTHORIZED',
        message: 'JWT token expired',
      });

      const result = await syncService.syncPendingOperations('user-401');

      expect(result.syncedCount).toBe(0);
      expect(result.pendingCount).toBe(1);

      // Queue items preserved for post-login retry
      const items = await getOfflineTransactions('user-401');
      expect(items[0].status).toBe('PENDING');
    });

    it('marks permanent validation errors (400/403) as FAILED', async () => {
      await saveOfflineTransaction({
        clientOperationId: 'client-op-403',
        operationType: 'CREATE',
        accountId: 'acc-forbidden',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 500,
        transactionDate: '2026-09-15',
        userId: 'user-403',
        status: 'PENDING',
        retryCount: 0,
        createdAt: new Date().toISOString(),
      });

      mockedProcessSyncTransaction.mockRejectedValueOnce({
        status: 403,
        code: 'ACCESS_DENIED',
        message: 'Account does not belong to user',
      });

      const result = await syncService.syncPendingOperations('user-403');

      expect(result.failedCount).toBe(1);

      const items = await getOfflineTransactions('user-403');
      expect(items[0].status).toBe('FAILED');
      expect(items[0].errorCode).toBe('ACCESS_DENIED');
    });

    it('handles 409 Conflict responses', async () => {
      await saveOfflineTransaction({
        clientOperationId: 'client-op-409',
        operationType: 'UPDATE',
        transactionId: 'tx-conflict-id',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 600,
        transactionDate: '2026-09-15',
        version: 1,
        userId: 'user-409',
        status: 'PENDING',
        retryCount: 0,
        createdAt: new Date().toISOString(),
      });

      mockedProcessSyncTransaction.mockRejectedValueOnce({
        status: 409,
        code: 'TRANSACTION_CONFLICT',
        message: 'The transaction was modified on the server.',
      });

      const result = await syncService.syncPendingOperations('user-409');

      expect(result.conflictCount).toBe(1);

      const items = await getOfflineTransactions('user-409');
      expect(items[0].status).toBe('CONFLICT');
      expect(items[0].errorCode).toBe('TRANSACTION_CONFLICT');
    });
  });

  describe('5. App Restart & Multi-User Safety', () => {
    it('preserves pending offline items across simulated app restarts', async () => {
      await saveOfflineTransaction({
        clientOperationId: 'op-durable',
        operationType: 'CREATE',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 1500,
        transactionDate: '2026-09-15',
        userId: 'user-restart',
        status: 'PENDING',
        retryCount: 0,
        createdAt: new Date().toISOString(),
      });

      // Simulate app restart by querying fresh queue
      const queue = await getPendingSyncQueue('user-restart');
      expect(queue.length).toBe(1);
      expect(queue[0].amount).toBe(1500);
    });
  });
});
