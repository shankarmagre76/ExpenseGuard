import {
  saveOfflineTransaction,
  getOfflineTransactions,
  getPendingSyncQueue,
  clearAllOfflineStorage,
} from '../src/storage/offlineStorage';
import { syncService } from '../src/services/syncService';
import { processSyncTransaction } from '../src/api/endpoints/syncApi';
import { getTransactionById } from '../src/api/endpoints/transactionApi';
import { OfflineTransactionItem } from '../src/types/offline';
import { TransactionResponse } from '../src/types/transaction';
import { calculatePayloadHash } from '../src/utils/payloadHash';

// Mock syncApi and transactionApi
jest.mock('../src/api/endpoints/syncApi', () => ({
  processSyncTransaction: jest.fn(),
}));

jest.mock('../src/api/endpoints/transactionApi', () => ({
  getTransactionById: jest.fn(),
}));

const mockedProcessSyncTransaction = processSyncTransaction as jest.MockedFunction<typeof processSyncTransaction>;
const mockedGetTransactionById = getTransactionById as jest.MockedFunction<typeof getTransactionById>;

describe('Phase 5.5 — Conflict Resolution', () => {
  beforeEach(async () => {
    await clearAllOfflineStorage();
    jest.clearAllMocks();
  });

  describe('1. Conflict Detection & Persistence', () => {
    it('marks local operation as CONFLICT when backend returns 409 TRANSACTION_CONFLICT', async () => {
      const offlineItem: OfflineTransactionItem = {
        clientOperationId: 'op-conflict-01',
        operationType: 'UPDATE',
        transactionId: 'tx-server-123',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 350,
        transactionDate: '2026-09-15',
        version: 1, // Stale local version
        userId: 'user-conflict-A',
        status: 'PENDING',
        retryCount: 0,
        createdAt: new Date().toISOString(),
      };

      await saveOfflineTransaction(offlineItem);

      mockedProcessSyncTransaction.mockRejectedValueOnce({
        status: 409,
        code: 'TRANSACTION_CONFLICT',
        message: 'Transaction version conflict on server',
      });

      const result = await syncService.syncPendingOperations('user-conflict-A');

      expect(result.conflictCount).toBe(1);

      const items = await getOfflineTransactions('user-conflict-A');
      expect(items.length).toBe(1);
      expect(items[0].status).toBe('CONFLICT');
      expect(items[0].errorCode).toBe('TRANSACTION_CONFLICT');
      expect(items[0].errorMessage).toBe('Transaction version conflict on server');
    });

    it('persists conflict state across simulated app restart and respects user isolation', async () => {
      await saveOfflineTransaction({
        clientOperationId: 'op-user-A-conflict',
        operationType: 'UPDATE',
        transactionId: 'tx-1',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 200,
        transactionDate: '2026-09-15',
        userId: 'user-A',
        status: 'CONFLICT',
        errorCode: 'TRANSACTION_CONFLICT',
        errorMessage: 'Version mismatch',
        retryCount: 0,
        createdAt: new Date().toISOString(),
      });

      // User A should have 1 conflict item
      const userAItems = await getOfflineTransactions('user-A');
      expect(userAItems.length).toBe(1);
      expect(userAItems[0].status).toBe('CONFLICT');

      // User B should have 0 items (User Isolation)
      const userBItems = await getOfflineTransactions('user-B');
      expect(userBItems.length).toBe(0);

      // Pending sync queue for User A should exclude CONFLICT items (stops auto retrying)
      const pendingQueueA = await getPendingSyncQueue('user-A');
      expect(pendingQueueA.length).toBe(0);
    });
  });

  describe('2. Resolution Option — KEEP SERVER', () => {
    it('discards local offline conflicting operation and leaves server transaction unchanged', async () => {
      const clientOpId = 'op-keep-server';
      await saveOfflineTransaction({
        clientOperationId: clientOpId,
        operationType: 'UPDATE',
        transactionId: 'tx-server-999',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 999,
        transactionDate: '2026-09-15',
        userId: 'user-keep-server',
        status: 'CONFLICT',
        errorCode: 'TRANSACTION_CONFLICT',
        retryCount: 0,
        createdAt: new Date().toISOString(),
      });

      await syncService.resolveKeepServer(clientOpId);

      const items = await getOfflineTransactions('user-keep-server');
      expect(items.length).toBe(0);
    });
  });

  describe('3. Resolution Option — KEEP LOCAL', () => {
    it('fetches server version, assigns fresh clientOperationId, recalculates payload hash, and syncs successfully', async () => {
      const origOpId = 'op-keep-local-orig';
      const userId = 'user-keep-local';

      await saveOfflineTransaction({
        clientOperationId: origOpId,
        operationType: 'UPDATE',
        transactionId: 'tx-server-456',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 500,
        transactionDate: '2026-09-15',
        description: 'Local override',
        version: 1, // Old stale version
        userId,
        status: 'CONFLICT',
        errorCode: 'TRANSACTION_CONFLICT',
        retryCount: 0,
        createdAt: new Date().toISOString(),
      });

      // Mock remote server return version = 3
      const mockServerTx: TransactionResponse = {
        id: 'tx-server-456',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        categoryName: 'Food & Dining',
        type: 'EXPENSE',
        amount: 400,
        transactionDate: '2026-09-15',
        description: 'Server updated notes',
        version: 3,
        createdAt: '2026-09-15T10:00:00Z',
        updatedAt: '2026-09-15T12:00:00Z',
      };
      mockedGetTransactionById.mockResolvedValueOnce(mockServerTx);

      // Mock backend sync endpoint processing new request
      mockedProcessSyncTransaction.mockResolvedValueOnce({
        clientOperationId: expect.any(String),
        status: 'PROCESSED',
        transactionId: 'tx-server-456',
        serverVersion: 4,
        message: 'Sync succeeded',
      });

      await syncService.resolveKeepLocal(origOpId, userId);

      // Original conflicting item should be replaced
      const items = await getOfflineTransactions(userId);
      expect(items.length).toBe(1);
      expect(items[0].clientOperationId).not.toBe(origOpId); // Fresh clientOperationId
      expect(items[0].version).toBe(4); // Updated to post-sync server version 4
      expect(items[0].status).toBe('SYNCED'); // Successfully synchronized
      expect(mockedGetTransactionById).toHaveBeenCalledWith('tx-server-456');
      expect(mockedProcessSyncTransaction).toHaveBeenCalledTimes(1);

      // Verify payload hash sent to backend contained version 3
      const syncCallReq = mockedProcessSyncTransaction.mock.calls[0][0];
      expect(syncCallReq.version).toBe(3);
      expect(syncCallReq.amount).toBe(500);
    });
  });

  describe('4. Resolution Option — EDIT & RESYNC', () => {
    it('applies user modifications, updates version from server, recalculates payload hash, and syncs', async () => {
      const origOpId = 'op-edit-resync-orig';
      const userId = 'user-edit-resync';

      await saveOfflineTransaction({
        clientOperationId: origOpId,
        operationType: 'UPDATE',
        transactionId: 'tx-server-789',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 100,
        transactionDate: '2026-09-15',
        version: 1,
        userId,
        status: 'CONFLICT',
        errorCode: 'TRANSACTION_CONFLICT',
        retryCount: 0,
        createdAt: new Date().toISOString(),
      });

      // Mock remote server tx (version 2)
      mockedGetTransactionById.mockResolvedValueOnce({
        id: 'tx-server-789',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        categoryName: 'Food & Dining',
        type: 'EXPENSE',
        amount: 120,
        transactionDate: '2026-09-15',
        version: 2,
        createdAt: '2026-09-15T10:00:00Z',
        updatedAt: '2026-09-15T11:00:00Z',
      });

      // Mock successful sync endpoint response
      mockedProcessSyncTransaction.mockResolvedValueOnce({
        clientOperationId: expect.any(String),
        status: 'PROCESSED',
        transactionId: 'tx-server-789',
        serverVersion: 3,
        message: 'Edited transaction synced',
      });

      await syncService.resolveEditAndResync(origOpId, userId, {
        accountId: 'acc-1',
        categoryId: 'cat-2', // Changed category
        type: 'EXPENSE',
        amount: 150, // Changed amount
        transactionDate: '2026-09-15',
        description: 'Reconciled amount',
      });

      const items = await getOfflineTransactions(userId);
      expect(items.length).toBe(1);
      expect(items[0].status).toBe('SYNCED');
      expect(items[0].amount).toBe(150);
      expect(items[0].categoryId).toBe('cat-2');
      expect(items[0].version).toBe(3); // Updated to post-sync server version 3

      // Verify payload hash calculation
      const syncReq = mockedProcessSyncTransaction.mock.calls[0][0];
      const expectedHash = calculatePayloadHash({
        clientOperationId: syncReq.clientOperationId,
        operationType: 'UPDATE',
        entityType: 'TRANSACTION',
        transactionId: 'tx-server-789',
        accountId: 'acc-1',
        categoryId: 'cat-2',
        type: 'EXPENSE',
        amount: 150,
        transactionDate: '2026-09-15',
        description: 'Reconciled amount',
        version: 2,
      });

      expect(calculatePayloadHash(syncReq)).toBe(expectedHash);
    });
  });

  describe('5. SYNC_OPERATION_PAYLOAD_MISMATCH Handling', () => {
    it('handles payload mismatch error code by placing transaction in CONFLICT state without auto retrying', async () => {
      const userId = 'user-mismatch';
      await saveOfflineTransaction({
        clientOperationId: 'op-mismatched-id',
        operationType: 'CREATE',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 250,
        transactionDate: '2026-09-15',
        userId,
        status: 'PENDING',
        retryCount: 0,
        createdAt: new Date().toISOString(),
      });

      mockedProcessSyncTransaction.mockRejectedValueOnce({
        status: 409,
        code: 'SYNC_OPERATION_PAYLOAD_MISMATCH',
        message: 'Payload hash does not match existing client operation ID record',
      });

      const result = await syncService.syncPendingOperations(userId);

      expect(result.conflictCount).toBe(1);

      const items = await getOfflineTransactions(userId);
      expect(items[0].status).toBe('CONFLICT');
      expect(items[0].errorCode).toBe('SYNC_OPERATION_PAYLOAD_MISMATCH');
    });
  });
});
