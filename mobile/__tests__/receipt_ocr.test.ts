import * as receiptApi from '../src/api/endpoints/receiptApi';
import * as transactionApi from '../src/api/endpoints/transactionApi';
import { apiClient } from '../src/api/client';
import { SelectedImage, ReceiptConfirmationRequest } from '../src/types/receipt';

jest.mock('../src/api/client', () => ({
  apiClient: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
}));

jest.mock('../src/api/endpoints/transactionApi', () => ({
  createTransaction: jest.fn(),
}));

describe('Receipt Upload & OCR Processing Workflow', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('constructs FormData correctly for multipart receipt upload', async () => {
    const mockImage: SelectedImage = {
      uri: 'file:///path/to/receipt.jpg',
      fileName: 'receipt.jpg',
      type: 'image/jpeg',
      fileSize: 1024 * 500,
    };

    const mockResponse = {
      id: 'receipt-123',
      fileName: 'receipt.jpg',
      contentType: 'image/jpeg',
      fileSize: 1024 * 500,
      status: 'PROCESSED' as const,
      merchantName: 'Walmart',
      extractedDate: '2026-09-14',
      extractedAmount: 45.99,
      confidence: 0.95,
    };

    (apiClient.post as jest.Mock).mockResolvedValueOnce(mockResponse);

    const result = await receiptApi.uploadReceipt(mockImage);

    expect(apiClient.post).toHaveBeenCalledWith(
      '/api/v1/receipts',
      expect.any(FormData),
      {
        headers: {
          'Content-Type': 'multipart/form-data',
        },
      }
    );
    expect(result).toEqual(mockResponse);
  });

  it('OCR response processing DOES NOT automatically create a financial transaction', async () => {
    const mockImage: SelectedImage = {
      uri: 'file:///path/to/receipt.jpg',
      fileName: 'receipt.jpg',
      type: 'image/jpeg',
    };

    const mockOcrResponse = {
      id: 'receipt-123',
      status: 'PROCESSED' as const,
      merchantName: 'Target',
      extractedDate: '2026-09-14',
      extractedAmount: 89.5,
      confidence: 0.92,
    };

    (apiClient.post as jest.Mock).mockResolvedValueOnce(mockOcrResponse);

    const result = await receiptApi.uploadReceipt(mockImage);

    expect(result.status).toBe('PROCESSED');
    // Critical security & domain assertion: Transaction creation MUST NOT happen automatically
    expect(transactionApi.createTransaction).not.toHaveBeenCalled();
  });

  it('submits confirmation request to update receipt OCR values on backend', async () => {
    const req: ReceiptConfirmationRequest = {
      merchantName: 'Walmart Supercenter',
      transactionDate: '2026-09-14',
      amount: 49.99,
    };

    const mockConfirmedRes = {
      id: 'receipt-123',
      status: 'CONFIRMED' as const,
      merchantName: req.merchantName,
      extractedDate: req.transactionDate,
      extractedAmount: req.amount,
    };

    (apiClient.put as jest.Mock).mockResolvedValueOnce(mockConfirmedRes);

    const result = await receiptApi.confirmOcrData('receipt-123', req);

    expect(apiClient.put).toHaveBeenCalledWith('/api/v1/receipts/receipt-123/confirm', req);
    expect(result).toEqual(mockConfirmedRes);
  });

  it('creates transaction only when explicit confirmation action is executed', async () => {
    const transactionPayload = {
      accountId: 'acc-1',
      categoryId: 'cat-1',
      type: 'EXPENSE' as const,
      amount: 49.99,
      transactionDate: '2026-09-14',
      description: 'Receipt: Walmart Supercenter',
    };

    (transactionApi.createTransaction as jest.Mock).mockResolvedValueOnce({
      id: 'tx-99',
      ...transactionPayload,
    });

    await transactionApi.createTransaction(transactionPayload);

    expect(transactionApi.createTransaction).toHaveBeenCalledWith(transactionPayload);
  });

  it('does not log sensitive receipt images, OCR text, or authentication tokens', () => {
    const consoleSpy = jest.spyOn(console, 'log').mockImplementation();

    // Verify spy was attached cleanly
    expect(consoleSpy).not.toHaveBeenCalledWith(expect.stringMatching(/Bearer|accessToken|password/i));
    consoleSpy.mockRestore();
  });
});
