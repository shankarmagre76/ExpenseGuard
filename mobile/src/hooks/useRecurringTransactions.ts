import { useState, useCallback, useEffect } from 'react';
import {
  RecurringTransactionResponse,
  RecurringTransactionRequest,
  RecurringExecutionResponse,
} from '../types/recurringTransaction';
import * as recurringApi from '../api/endpoints/recurringTransactionApi';
import { AppError } from '../types/api';

export const useRecurringTransactions = () => {
  const [recurringTransactions, setRecurringTransactions] = useState<RecurringTransactionResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await recurringApi.getRecurringTransactions();
      setRecurringTransactions(data);
    } catch (err: any) {
      const appErr = err as AppError;
      setError(appErr.message || 'Failed to load recurring transactions.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const addRecurringTransaction = useCallback(
    async (data: RecurringTransactionRequest): Promise<RecurringTransactionResponse> => {
      try {
        const created = await recurringApi.createRecurringTransaction(data);
        await refresh();
        return created;
      } catch (err: any) {
        const appErr = err as AppError;
        throw new Error(appErr.message || 'Failed to create recurring transaction.');
      }
    },
    [refresh]
  );

  const editRecurringTransaction = useCallback(
    async (id: string, data: RecurringTransactionRequest): Promise<RecurringTransactionResponse> => {
      try {
        const updated = await recurringApi.updateRecurringTransaction(id, data);
        await refresh();
        return updated;
      } catch (err: any) {
        const appErr = err as AppError;
        throw new Error(appErr.message || 'Failed to update recurring transaction.');
      }
    },
    [refresh]
  );

  const removeRecurringTransaction = useCallback(
    async (id: string): Promise<void> => {
      try {
        await recurringApi.deleteRecurringTransaction(id);
        await refresh();
      } catch (err: any) {
        const appErr = err as AppError;
        throw new Error(appErr.message || 'Failed to delete recurring transaction.');
      }
    },
    [refresh]
  );

  const toggleStatus = useCallback(
    async (id: string, active: boolean): Promise<RecurringTransactionResponse> => {
      try {
        const updated = await recurringApi.toggleRecurringTransactionStatus(id, active);
        setRecurringTransactions((prev) =>
          prev.map((item) => (item.id === id ? updated : item))
        );
        return updated;
      } catch (err: any) {
        const appErr = err as AppError;
        throw new Error(appErr.message || 'Failed to update status.');
      }
    },
    []
  );

  const triggerExecution = useCallback(
    async (id: string): Promise<RecurringExecutionResponse> => {
      try {
        const res = await recurringApi.triggerManualExecution(id);
        await refresh();
        return res;
      } catch (err: any) {
        const appErr = err as AppError;
        throw new Error(appErr.message || 'Failed to execute recurring transaction.');
      }
    },
    [refresh]
  );

  return {
    recurringTransactions,
    loading,
    error,
    refresh,
    addRecurringTransaction,
    editRecurringTransaction,
    removeRecurringTransaction,
    toggleStatus,
    triggerExecution,
  };
};
