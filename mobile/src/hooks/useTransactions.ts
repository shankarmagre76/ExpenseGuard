import { useState, useCallback, useEffect } from 'react';
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
import { AppError } from '../types/api';

export const useTransactions = (initialFilters?: TransactionFilters) => {
  const [transactions, setTransactions] = useState<TransactionResponse[]>([]);
  const [filters, setFilters] = useState<TransactionFilters>(initialFilters || { page: 0, size: 20 });
  const [loading, setLoading] = useState<boolean>(true);
  const [loadingMore, setLoadingMore] = useState<boolean>(false);
  const [hasMore, setHasMore] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchPage = useCallback(
    async (currentPage: number, currentFilters: TransactionFilters, isLoadMore: boolean = false) => {
      if (isLoadMore) {
        setLoadingMore(true);
      } else {
        setLoading(true);
      }
      setError(null);

      try {
        const pageData = await getTransactions({
          ...currentFilters,
          page: currentPage,
          size: currentFilters.size || 20,
        });

        if (isLoadMore) {
          setTransactions((prev) => [...prev, ...pageData.content]);
        } else {
          setTransactions(pageData.content);
        }

        setHasMore(!pageData.last && pageData.content.length > 0);
      } catch (err: any) {
        const appErr = err as AppError;
        setError(appErr.message || 'Failed to load transactions.');
      } finally {
        setLoading(false);
        setLoadingMore(false);
      }
    },
    []
  );

  const refresh = useCallback(() => {
    const resetFilters = { ...filters, page: 0 };
    setFilters(resetFilters);
    return fetchPage(0, resetFilters, false);
  }, [filters, fetchPage]);

  useEffect(() => {
    fetchPage(0, filters, false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []); // Initial load

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

  const addTx = async (request: TransactionRequest): Promise<TransactionResponse> => {
    const created = await createTransaction(request);
    await refresh();
    return created;
  };

  const editTx = async (id: string, request: TransactionRequest): Promise<TransactionResponse> => {
    const updated = await updateTransaction(id, request);
    await refresh();
    return updated;
  };

  const removeTx = async (id: string): Promise<void> => {
    await deleteTransaction(id);
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
  };
};
