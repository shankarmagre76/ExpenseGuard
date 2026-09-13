import { apiClient } from '../client';
import {
  ReceiptResponse,
  ReceiptConfirmationRequest,
  SelectedImage,
} from '../../types/receipt';

export const uploadReceipt = async (image: SelectedImage): Promise<ReceiptResponse> => {
  const formData = new FormData();
  formData.append('file', {
    uri: image.uri,
    name: image.fileName || 'receipt.jpg',
    type: image.type || 'image/jpeg',
  } as any);

  return await apiClient.post<ReceiptResponse>('/api/v1/receipts', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  });
};

export const getReceiptById = async (id: string): Promise<ReceiptResponse> => {
  return await apiClient.get<ReceiptResponse>(`/api/v1/receipts/${id}`);
};

export const deleteReceipt = async (id: string): Promise<void> => {
  await apiClient.delete(`/api/v1/receipts/${id}`);
};

export const processOcr = async (id: string): Promise<ReceiptResponse> => {
  return await apiClient.post<ReceiptResponse>(`/api/v1/receipts/${id}/process`);
};

export const confirmOcrData = async (
  id: string,
  request: ReceiptConfirmationRequest
): Promise<ReceiptResponse> => {
  return await apiClient.put<ReceiptResponse>(`/api/v1/receipts/${id}/confirm`, request);
};
