import { useState, useCallback } from 'react';
import {
  ReceiptResponse,
  ReceiptConfirmationRequest,
  SelectedImage,
} from '../types/receipt';
import * as receiptApi from '../api/endpoints/receiptApi';
import { AppError } from '../types/api';

export const useReceipts = () => {
  const [uploading, setUploading] = useState<boolean>(false);
  const [processingOcr, setProcessingOcr] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const uploadAndScan = useCallback(async (image: SelectedImage): Promise<ReceiptResponse> => {
    try {
      setUploading(true);
      setError(null);
      const response = await receiptApi.uploadReceipt(image);
      return response;
    } catch (err: any) {
      const appErr = err as AppError;
      const msg = appErr.message || 'Failed to upload receipt image.';
      setError(msg);
      throw new Error(msg);
    } finally {
      setUploading(false);
    }
  }, []);

  const confirmReceiptData = useCallback(
    async (id: string, request: ReceiptConfirmationRequest): Promise<ReceiptResponse> => {
      try {
        setProcessingOcr(true);
        setError(null);
        const response = await receiptApi.confirmOcrData(id, request);
        return response;
      } catch (err: any) {
        const appErr = err as AppError;
        const msg = appErr.message || 'Failed to confirm receipt details.';
        setError(msg);
        throw new Error(msg);
      } finally {
        setProcessingOcr(false);
      }
    },
    []
  );

  const deleteReceiptFile = useCallback(async (id: string): Promise<void> => {
    try {
      setError(null);
      await receiptApi.deleteReceipt(id);
    } catch (err: any) {
      const appErr = err as AppError;
      setError(appErr.message || 'Failed to delete receipt.');
    }
  }, []);

  return {
    uploading,
    processingOcr,
    error,
    setError,
    uploadAndScan,
    confirmReceiptData,
    deleteReceiptFile,
  };
};
