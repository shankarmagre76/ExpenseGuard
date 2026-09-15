import { apiClient } from '../client';
import {
  SyncTransactionRequest,
  SyncTransactionResponse,
  BatchSyncRequest,
  BatchSyncResponse,
  SyncStatusResponse,
} from '../../types/offline';

export const processSyncTransaction = async (
  request: SyncTransactionRequest
): Promise<SyncTransactionResponse> => {
  return apiClient.post<SyncTransactionResponse>('/api/v1/sync/transactions', request);
};

export const processBatchSyncTransactions = async (
  request: BatchSyncRequest
): Promise<BatchSyncResponse> => {
  return apiClient.post<BatchSyncResponse>('/api/v1/sync/transactions/batch', request);
};

export const getSyncStatus = async (
  clientOperationId: string
): Promise<SyncStatusResponse> => {
  return apiClient.get<SyncStatusResponse>(`/api/v1/sync/status/${clientOperationId}`);
};
