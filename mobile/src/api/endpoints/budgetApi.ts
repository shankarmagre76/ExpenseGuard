import { apiClient } from '../client';
import { BudgetRequest, BudgetResponse } from '../../types/budget';

export const getBudgets = async (month?: string): Promise<BudgetResponse[]> => {
  const params: Record<string, string> = {};
  if (month) {
    params.month = month;
  }
  return await apiClient.get<BudgetResponse[]>('/api/v1/budgets', { params });
};

export const getBudgetById = async (id: string): Promise<BudgetResponse> => {
  return await apiClient.get<BudgetResponse>(`/api/v1/budgets/${id}`);
};

export const getBudgetSummary = async (id: string): Promise<BudgetResponse> => {
  return await apiClient.get<BudgetResponse>(`/api/v1/budgets/${id}/summary`);
};

export const createBudget = async (data: BudgetRequest): Promise<BudgetResponse> => {
  return await apiClient.post<BudgetResponse>('/api/v1/budgets', data);
};

export const updateBudget = async (id: string, data: BudgetRequest): Promise<BudgetResponse> => {
  return await apiClient.put<BudgetResponse>(`/api/v1/budgets/${id}`, data);
};

export const deleteBudget = async (id: string): Promise<void> => {
  await apiClient.delete(`/api/v1/budgets/${id}`);
};
