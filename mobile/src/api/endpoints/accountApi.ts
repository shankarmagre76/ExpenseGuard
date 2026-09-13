import { apiClient } from '../client';
import { AccountRequest, AccountResponse } from '../../types/account';

export const getAccounts = async (): Promise<AccountResponse[]> => {
  return apiClient.get<AccountResponse[]>('/api/v1/accounts');
};

export const getAccountById = async (id: string): Promise<AccountResponse> => {
  return apiClient.get<AccountResponse>(`/api/v1/accounts/${id}`);
};

export const createAccount = async (data: AccountRequest): Promise<AccountResponse> => {
  return apiClient.post<AccountResponse>('/api/v1/accounts', data);
};

export const updateAccount = async (id: string, data: AccountRequest): Promise<AccountResponse> => {
  return apiClient.put<AccountResponse>(`/api/v1/accounts/${id}`, data);
};

export const deleteAccount = async (id: string): Promise<void> => {
  return apiClient.delete<void>(`/api/v1/accounts/${id}`);
};
