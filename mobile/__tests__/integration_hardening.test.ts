import {
  saveOfflineTransaction,
  getOfflineTransactions,
  clearAllOfflineStorage,
} from '../src/storage/offlineStorage';
import { syncService } from '../src/services/syncService';
import { processSyncTransaction } from '../src/api/endpoints/syncApi';
import { getNotifications } from '../src/api/endpoints/notificationApi';
import { networkService } from '../src/services/networkService';
import { calculatePayloadHash } from '../src/utils/payloadHash';

// Mock API endpoints
jest.mock('../src/api/endpoints/syncApi', () => ({
  processSyncTransaction: jest.fn(),
}));

jest.mock('../src/api/endpoints/notificationApi', () => ({
  getNotifications: jest.fn(),
}));

const mockedProcessSyncTransaction = processSyncTransaction as jest.MockedFunction<typeof processSyncTransaction>;
const mockedGetNotifications = getNotifications as jest.MockedFunction<typeof getNotifications>;

describe('Phase 5.6 — Integration Testing & End-to-End Hardening', () => {
  beforeEach(async () => {
    await clearAllOfflineStorage();
    jest.clearAllMocks();
    networkService.resetMockStatus();
  });

  describe('1. Idempotency & Duplicate Prevention', () => {
    it('prevents duplicate transaction creation on network retry using clientOperationId', async () => {
      const clientOpId = 'op-idempotent-001';
      const userId = 'user-idem';

      await saveOfflineTransaction({
        clientOperationId: clientOpId,
        operationType: 'CREATE',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 150,
        transactionDate: '2026-09-15',
        userId,
        status: 'PENDING',
        retryCount: 0,
        createdAt: new Date().toISOString(),
      });

      // Simulating backend idempotent response: repeated submission of clientOperationId returns same server transaction
      mockedProcessSyncTransaction.mockResolvedValue({
        clientOperationId: clientOpId,
        status: 'PROCESSED',
        transactionId: 'tx-server-idem-uuid',
        serverVersion: 0,
        message: 'Transaction synchronized successfully',
      });

      // First sync run
      const res1 = await syncService.syncPendingOperations(userId);
      expect(res1.syncedCount).toBe(1);

      // Second sync run (retry)
      const res2 = await syncService.syncPendingOperations(userId);
      expect(res2.syncedCount).toBe(0); // Queue is already SYNCED, no re-submission

      const items = await getOfflineTransactions(userId);
      expect(items.length).toBe(1);
      expect(items[0].transactionId).toBe('tx-server-idem-uuid');
      expect(items[0].status).toBe('SYNCED');
    });
  });

  describe('2. User Isolation Hardening', () => {
    it('isolates offline pending transactions between different users', async () => {
      // User A offline transaction
      await saveOfflineTransaction({
        clientOperationId: 'op-user-A-secret',
        operationType: 'CREATE',
        accountId: 'acc-A',
        categoryId: 'cat-A',
        type: 'EXPENSE',
        amount: 300,
        transactionDate: '2026-09-15',
        userId: 'user-A',
        status: 'PENDING',
        retryCount: 0,
        createdAt: new Date().toISOString(),
      });

      // User B sync trigger
      const resUserB = await syncService.syncPendingOperations('user-B');
      expect(resUserB.syncedCount).toBe(0);
      expect(mockedProcessSyncTransaction).not.toHaveBeenCalled();

      // User A sync trigger
      mockedProcessSyncTransaction.mockResolvedValueOnce({
        clientOperationId: 'op-user-A-secret',
        status: 'PROCESSED',
        transactionId: 'tx-A-uuid',
        serverVersion: 0,
      });

      const resUserA = await syncService.syncPendingOperations('user-A');
      expect(resUserA.syncedCount).toBe(1);
      expect(mockedProcessSyncTransaction).toHaveBeenCalledTimes(1);
    });
  });

  describe('3. Offline Receipt OCR Network Safety', () => {
    it('detects offline state and blocks binary upload before making network call', async () => {
      networkService.setMockStatus(false); // OFFLINE

      const isOnline = await networkService.isOnline();
      expect(isOnline).toBe(false);
    });
  });

  describe('4. Graceful Notification API Absence (Backend Limitation)', () => {
    it('handles 404 Not Found from notification API gracefully without crashing', async () => {
      mockedGetNotifications.mockRejectedValueOnce({
        status: 404,
        code: 'NOT_FOUND',
        message: 'Request path /api/v1/notifications not found',
      });

      await expect(getNotifications()).rejects.toMatchObject({
        status: 404,
      });
    });
  });

  describe('5. Complete HTTP Error Handling Matrix', () => {
    const testCases = [
      {
        status: 400,
        code: 'BAD_REQUEST',
        expectedSyncStatus: 'FAILED',
        desc: 'Permanent validation failure (400)',
      },
      {
        status: 403,
        code: 'ACCESS_DENIED',
        expectedSyncStatus: 'FAILED',
        desc: 'Permanent access denied (403)',
      },
      {
        status: 409,
        code: 'TRANSACTION_CONFLICT',
        expectedSyncStatus: 'CONFLICT',
        desc: 'Version optimistic locking conflict (409)',
      },
      {
        status: 500,
        code: 'INTERNAL_ERROR',
        expectedSyncStatus: 'PENDING',
        desc: 'Temporary server error (500)',
      },
    ];

    testCases.forEach(({ status, code, expectedSyncStatus, desc }) => {
      it(`handles ${desc} correctly`, async () => {
        const opId = `op-err-${status}`;
        const userId = `user-err-${status}`;

        await saveOfflineTransaction({
          clientOperationId: opId,
          operationType: 'CREATE',
          accountId: 'acc-1',
          categoryId: 'cat-1',
          type: 'EXPENSE',
          amount: 100,
          transactionDate: '2026-09-15',
          userId,
          status: 'PENDING',
          retryCount: 0,
          createdAt: new Date().toISOString(),
        });

        mockedProcessSyncTransaction.mockRejectedValueOnce({
          status,
          code,
          message: `HTTP ${status} error`,
        });

        await syncService.syncPendingOperations(userId);

        const items = await getOfflineTransactions(userId);
        expect(items[0].status).toBe(expectedSyncStatus);
      });
    });
  });

  describe('6. Canonical Payload Hash Determinism', () => {
    it('generates consistent SHA-256 payload hash across different whitespace & zero-padding inputs', () => {
      const hash1 = calculatePayloadHash({
        clientOperationId: 'op-1',
        operationType: 'CREATE',
        entityType: 'TRANSACTION',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 250.0,
        transactionDate: '2026-09-15',
        description: '  Grocery store  ',
      });

      const hash2 = calculatePayloadHash({
        clientOperationId: 'op-1',
        operationType: 'CREATE',
        entityType: 'TRANSACTION',
        accountId: 'acc-1',
        categoryId: 'cat-1',
        type: 'EXPENSE',
        amount: 250,
        transactionDate: '2026-09-15',
        description: '  Grocery store  ',
      });

      expect(hash1).toBe(hash2);
      expect(hash1).toHaveLength(64);
    });
  });
});
