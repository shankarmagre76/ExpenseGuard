export type ReceiptStatus = 'UPLOADED' | 'PROCESSING' | 'PROCESSED' | 'FAILED' | 'CONFIRMED';

export interface ReceiptResponse {
  id: string;
  fileName: string;
  contentType: string;
  fileSize: number;
  status: ReceiptStatus;
  merchantName?: string | null;
  extractedDate?: string | null;
  extractedAmount?: number | null;
  confidence?: number | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface ReceiptConfirmationRequest {
  merchantName: string;
  transactionDate: string; // YYYY-MM-DD
  amount: number;
}

export interface OcrResult {
  merchantName: string;
  transactionDate: string;
  amount: number;
  confidence: number;
  rawText?: string;
}

export interface SelectedImage {
  uri: string;
  fileName?: string;
  type?: string;
  fileSize?: number;
}
