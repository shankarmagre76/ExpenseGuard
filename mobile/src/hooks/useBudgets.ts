import { useState, useCallback, useEffect } from 'react';
import { BudgetResponse, BudgetRequest } from '../types/budget';
import * as budgetApi from '../api/endpoints/budgetApi';
import { AppError } from '../types/api';
import { getCurrentMonth } from '../utils/dateFormatter';

export const useBudgets = (initialMonth: string = getCurrentMonth()) => {
  const [selectedMonth, setSelectedMonth] = useState<string>(initialMonth);
  const [budgets, setBudgets] = useState<BudgetResponse[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchBudgets = useCallback(async (month: string) => {
    try {
      setLoading(true);
      setError(null);
      const data = await budgetApi.getBudgets(month);
      setBudgets(data);
    } catch (err: any) {
      const appErr = err as AppError;
      setError(appErr.message || 'Failed to load budgets.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchBudgets(selectedMonth);
  }, [fetchBudgets, selectedMonth]);

  const refreshBudgets = useCallback(() => {
    return fetchBudgets(selectedMonth);
  }, [fetchBudgets, selectedMonth]);

  const addBudget = useCallback(
    async (data: BudgetRequest): Promise<BudgetResponse> => {
      try {
        const created = await budgetApi.createBudget(data);
        if (data.month === selectedMonth) {
          await fetchBudgets(selectedMonth);
        }
        return created;
      } catch (err: any) {
        const appErr = err as AppError;
        throw new Error(appErr.message || 'Failed to create budget.');
      }
    },
    [fetchBudgets, selectedMonth]
  );

  const editBudget = useCallback(
    async (id: string, data: BudgetRequest): Promise<BudgetResponse> => {
      try {
        const updated = await budgetApi.updateBudget(id, data);
        await fetchBudgets(selectedMonth);
        return updated;
      } catch (err: any) {
        const appErr = err as AppError;
        throw new Error(appErr.message || 'Failed to update budget.');
      }
    },
    [fetchBudgets, selectedMonth]
  );

  const removeBudget = useCallback(
    async (id: string): Promise<void> => {
      try {
        await budgetApi.deleteBudget(id);
        await fetchBudgets(selectedMonth);
      } catch (err: any) {
        const appErr = err as AppError;
        throw new Error(appErr.message || 'Failed to delete budget.');
      }
    },
    [fetchBudgets, selectedMonth]
  );

  return {
    selectedMonth,
    setSelectedMonth,
    budgets,
    loading,
    error,
    refreshBudgets,
    addBudget,
    editBudget,
    removeBudget,
  };
};
