import { useState, useCallback, useEffect, useContext } from 'react';
import {
  TransactionResponse,
  TransactionRequest,
  TransactionFilters,
} from '../types/transaction';
import {
  getTransactions,
  createTransaction,
  updateTransaction,
  deleteTransaction,
} from '../api/endpoints/transactionApi';
import { AuthContext } from '../context/AuthContext';
import { networkService } from '../services/networkService';
import { syncService } from '../services/syncService';
import {
  getOfflineTransactions,
  saveOfflineTransaction,
  updateOfflineTransactionStatus,
  removeOfflineTransaction,
} from '../storage/offlineStorage';
import { OfflineTransactionItem } from '../types/offline';
import { generateUUID } from '../utils/uuid';
import { AppError } from '../types/api';

export const useTransactions = (initialFilters?: TransactionFilters) => {
  const { user } = useContext(AuthContext);
  const userId = user?.userId;

  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [filters, setFilters] = useState<TransactionFilters>(initialFilters || { page: 0, size: 20 });
  const [loading, setLoading] = useState<boolean>(true);
  const [loadingMore, setLoadingMore] = useState<boolean>(false);
  const [hasMore, setHasMore] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const mapOfflineItemToResponse = (item: OfflineTransactionItem): TransactionResponse => {
    return {
      id: item.transactionId || item.clientOperationId,
      clientOperationId: item.clientOperationId,
      accountId: item.accountId,
      categoryId: item.categoryId,
      categoryName: item.categoryName || 'Transaction',
      type: item.type,
      amount: item.amount,
      transactionDate: item.transactionDate,
      description: item.description,
      version: item.version,
      syncStatus: item.status,
      syncErrorMessage: item.errorMessage,
      createdAt: item.createdAt,
      updatedAt: item.createdAt,
    };
  };

  const mergeLocalAndRemoteTransactions = useCallback(
    async (remoteList: TransactionResponse[], currentUserId?: string): Promise<TransactionResponse[]> => {
      if (!currentUserId) return remoteList;
      const offlineItems = await getOfflineTransactions(currentUserId);
      
      // Filter offline items that are pending, failed, syncing, or conflict
      const activeUnsyncedItems = offlineItems.filter(
        (item) => item.status === 'PENDING' || item.status === 'SYNCING' || item.status === 'FAILED' || item.status === 'CONFLICT'
      );

      const localResponses = activeUnsyncedItems.map(mapOfflineItemToResponse);

      // Avoid duplicate display if already present in remote list
      const remoteIds = new Set(remoteList.map((tx) => tx.id));
      const remoteClientOpIds = new Set(remoteList.map((tx) => tx.clientOperationId).filter(Boolean));

      const uniqueLocal = localResponses.filter(
        (loc) => !remoteIds.has(loc.id) && !remoteClientOpIds.has(loc.clientOperationId)
      );

      // Unsynced local transactions appear at the top
      return [...uniqueLocal, ...remoteList];
    },
    []
  );

  const fetchPage = useCallback(
    async (currentPage: number, currentFilters: TransactionFilters, isLoadMore: boolean = false) => {
      if (isLoadMore) {
        setLoadingMore(true);
      } else {
        setLoading(true);
      }
      setError(null);

      const isOnline = await networkService.isOnline();

      if (!isOnline) {
        // Offline fetch path: load local offline items
        if (userId) {
          const offlineItems = await getOfflineTransactions(userId);
          const localList = offlineItems.map(mapOfflineItemToResponse);
          setTransactions(localList);
        } else {
          setTransactions([]);
        }
        setHasMore(false);
        setLoading(false);
        setLoadingMore(false);
        return;
      }

      try {
        const pageData = await getTransactions({
          ...currentFilters,
          page: currentPage,
          size: currentFilters.size || 20,
        });

        const merged = await mergeLocalAndRemoteTransactions(pageData.content, userId);

        if (isLoadMore) {
          setTransactions((prev) => [...prev, ...pageData.content]);
        } else {
          setTransactions(merged);
        }

        setHasMore(!pageData.last && pageData.content.length > 0);
      } catch (err: any) {
        const appErr = err as AppError;
        if (userId) {
          // Fallback to local offline items on network error
          const offlineItems = await getOfflineTransactions(userId);
          const localList = offlineItems.map(mapOfflineItemToResponse);
          setTransactions(localList);
        } else {
          setError(appErr.message || 'Failed to load transactions.');
        }
      } finally {
        setLoading(false);
        setLoadingMore(false);
      }
    },
    [userId, mergeLocalAndRemoteTransactions]
  );

  const refresh = useCallback(() => {
    const resetFilters = { ...filters, page: 0 };
    setFilters(resetFilters);
    return fetchPage(0, resetFilters, false);
  }, [filters, fetchPage]);

  useEffect(() => {
    fetchPage(0, filters, false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId]); // Initial load or user switch

  const applyFilters = (newFilters: Partial<TransactionFilters>) => {
    const updated = { ...filters, ...newFilters, page: 0 };
    setFilters(updated);
    fetchPage(0, updated, false);
  };

  const clearFilters = () => {
    const reset = { page: 0, size: 20 };
    setFilters(reset);
    fetchPage(0, reset, false);
  };

  const loadMore = () => {
    if (loading || loadingMore || !hasMore) return;
    const nextPage = (filters.page || 0) + 1;
    const updated = { ...filters, page: nextPage };
    setFilters(updated);
    fetchPage(nextPage, updated, true);
  };

  const validateTransactionData = (request: TransactionRequest) => {
    if (!request.accountId) throw new Error('Account selection is required.');
    if (!request.categoryId) throw new Error('Category selection is required.');
    if (!request.type || (request.type !== 'EXPENSE' && request.type !== 'INCOME')) {
      throw new Error('Valid transaction type (EXPENSE or INCOME) is required.');
    }
    const numAmt = typeof request.amount === 'string' ? parseFloat(request.amount) : request.amount;
    if (isNaN(numAmt) || numAmt <= 0) {
      throw new Error('Transaction amount must be greater than zero.');
    }
    if (!request.transactionDate) throw new Error('Transaction date is required.');
  };

  const addTx = async (request: TransactionRequest): Promise<TransactionResponse> => {
    validateTransactionData(request);

    if (!userId) {
      throw new Error('Authenticated user session required.');
    }

    const clientOpId = request.clientOperationId || generateUUID();
    const isOnline = await networkService.isOnline();

    if (isOnline) {
      try {
        const created = await createTransaction({ ...request, clientOperationId: clientOpId });
        await refresh();
        return created;
      } catch (err: any) {
        const appErr = err as AppError;
        // If permanent error (400, 403, validation), rethrow
        if (appErr.status === 400 || appErr.status === 403) {
          throw err;
        }
        // Fallback to offline store on network outage during request
      }
    }

    // OFFLINE CREATION PATH
    const numAmt = typeof request.amount === 'string' ? parseFloat(request.amount) : request.amount;
    const offlineItem: OfflineTransactionItem = {
      clientOperationId: clientOpId,
      operationType: 'CREATE',
      entityType: 'TRANSACTION',
      accountId: request.accountId,
      categoryId: request.categoryId,
      type: request.type as 'EXPENSE' | 'INCOME',
      amount: numAmt,
      transactionDate: request.transactionDate,
      description: request.description,
      userId,
      status: 'PENDING',
      retryCount: 0,
      createdAt: new Date().toISOString(),
    };

    await saveOfflineTransaction(offlineItem);
    await refresh();

    return mapOfflineItemToResponse(offlineItem);
  };

  const editTx = async (id: string, request: TransactionRequest): Promise<TransactionResponse> => {
    validateTransactionData(request);

    if (!userId) {
      throw new Error('Authenticated user session required.');
    }

    const isOnline = await networkService.isOnline();

    if (isOnline) {
      try {
        const updated = await updateTransaction(id, request);
        await refresh();
        return updated;
      } catch (err: any) {
        const appErr = err as AppError;
        if (appErr.status === 400 || appErr.status === 403 || appErr.status === 409) {
          throw err;
        }
      }
    }

    // OFFLINE UPDATE PATH
    const clientOpId = request.clientOperationId || generateUUID();
    const numAmt = typeof request.amount === 'string' ? parseFloat(request.amount) : request.amount;
    const offlineItem: OfflineTransactionItem = {
      clientOperationId: clientOpId,
      operationType: 'UPDATE',
      entityType: 'TRANSACTION',
      transactionId: id,
      accountId: request.accountId,
      categoryId: request.categoryId,
      type: request.type as 'EXPENSE' | 'INCOME',
      amount: numAmt,
      transactionDate: request.transactionDate,
      description: request.description,
      userId,
      status: 'PENDING',
      retryCount: 0,
      createdAt: new Date().toISOString(),
    };

    await saveOfflineTransaction(offlineItem);
    await refresh();
    return mapOfflineItemToResponse(offlineItem);
  };

  const removeTx = async (id: string): Promise<void> => {
    if (!userId) return;

    const isOnline = await networkService.isOnline();

    if (isOnline) {
      try {
        await deleteTransaction(id);
        // Clean local offline queue if it was queued locally
        await removeOfflineTransaction(id);
        await refresh();
        return;
      } catch (err: any) {
        const appErr = err as AppError;
        if (appErr.status === 400 || appErr.status === 403) {
          throw err;
        }
      }
    }

    // OFFLINE DELETE PATH
    const clientOpId = generateUUID();
    const offlineItem: OfflineTransactionItem = {
      clientOperationId: clientOpId,
      operationType: 'DELETE',
      entityType: 'TRANSACTION',
      transactionId: id,
      accountId: '',
      categoryId: '',
      type: 'EXPENSE',
      amount: 0,
      transactionDate: new Date().toISOString().split('T')[0],
      userId,
      status: 'PENDING',
      retryCount: 0,
      createdAt: new Date().toISOString(),
    };

    await saveOfflineTransaction(offlineItem);
    await refresh();
  };

  const retrySingleSync = async (clientOperationId: string): Promise<void> => {
    if (!userId) return;
    await updateOfflineTransactionStatus(clientOperationId, { status: 'PENDING', retryCount: 0 });
    await syncService.syncPendingOperations(userId);
    await refresh();
  };

  return {
    transactions,
    loading,
    loadingMore,
    hasMore,
    error,
    filters,
    refresh,
    applyFilters,
    clearFilters,
    loadMore,
    addTransaction: addTx,
    editTransaction: editTx,
    deleteTransaction: removeTx,
    retrySingleSync,
  };
};
