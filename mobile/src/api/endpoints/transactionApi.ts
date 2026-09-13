import { apiClient } from '../client';
import {
  TransactionRequest,
  TransactionResponse,
  TransactionFilters,
  PageResponse,
} from '../../types/transaction';

export const getTransactions = async (
  filters?: TransactionFilters
): Promise<PageResponse<TransactionResponse>> => {
  const params: Record<string, any> = {};
  if (filters) {
    if (filters.type) params.type = filters.type;
    if (filters.accountId) params.accountId = filters.accountId;
    if (filters.categoryId) params.categoryId = filters.categoryId;
    if (filters.fromDate) params.fromDate = filters.fromDate;
    if (filters.toDate) params.toDate = filters.toDate;
    if (filters.page !== undefined) params.page = filters.page;
    if (filters.size !== undefined) params.size = filters.size;
  }

  return apiClient.get<PageResponse<TransactionResponse>>('/api/v1/transactions', { params });
};

export const getTransactionById = async (id: string): Promise<TransactionResponse> => {
  return apiClient.get<TransactionResponse>(`/api/v1/transactions/${id}`);
};

export const createTransaction = async (
  data: TransactionRequest
): Promise<TransactionResponse> => {
  return apiClient.post<TransactionResponse>('/api/v1/transactions', data);
};

export const updateTransaction = async (
  id: string,
  data: TransactionRequest
): Promise<TransactionResponse> => {
  return apiClient.put<TransactionResponse>(`/api/v1/transactions/${id}`, data);
};

export const deleteTransaction = async (id: string): Promise<void> => {
  return apiClient.delete<void>(`/api/v1/transactions/${id}`);
};
