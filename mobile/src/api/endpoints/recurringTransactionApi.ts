import { apiClient } from '../client';
import {
  RecurringTransactionRequest,
  RecurringTransactionResponse,
  RecurringTransactionStatusRequest,
  RecurringExecutionResponse,
} from '../../types/recurringTransaction';

export const getRecurringTransactions = async (): Promise<RecurringTransactionResponse[]> => {
  return await apiClient.get<RecurringTransactionResponse[]>('/api/v1/recurring-transactions');
};

export const getRecurringTransactionById = async (id: string): Promise<RecurringTransactionResponse> => {
  return await apiClient.get<RecurringTransactionResponse>(`/api/v1/recurring-transactions/${id}`);
};

export const createRecurringTransaction = async (
  data: RecurringTransactionRequest
): Promise<RecurringTransactionResponse> => {
  return await apiClient.post<RecurringTransactionResponse>('/api/v1/recurring-transactions', data);
};

export const updateRecurringTransaction = async (
  id: string,
  data: RecurringTransactionRequest
): Promise<RecurringTransactionResponse> => {
  return await apiClient.put<RecurringTransactionResponse>(`/api/v1/recurring-transactions/${id}`, data);
};

export const deleteRecurringTransaction = async (id: string): Promise<void> => {
  await apiClient.delete(`/api/v1/recurring-transactions/${id}`);
};

export const toggleRecurringTransactionStatus = async (
  id: string,
  active: boolean
): Promise<RecurringTransactionResponse> => {
  const payload: RecurringTransactionStatusRequest = { active };
  return await apiClient.put<RecurringTransactionResponse>(`/api/v1/recurring-transactions/${id}/status`, payload);
};

export const triggerManualExecution = async (id: string): Promise<RecurringExecutionResponse> => {
  return await apiClient.post<RecurringExecutionResponse>(`/api/v1/recurring-transactions/${id}/execute`);
};
