/**
 * Transaction type definitions matching Spring Boot backend DTOs & Pageable responses.
 */

export type TransactionType = 'INCOME' | 'EXPENSE' | 'TRANSFER';

export interface TransactionRequest {
  accountId: string;
  categoryId: string;
  type: TransactionType;
  amount: number | string;
  transactionDate: string; // ISO Date YYYY-MM-DD
  description?: string;
  clientOperationId?: string;
}

export interface TransactionResponse {
  id: string;
  accountId: string;
  categoryId: string;
  categoryName: string;
  type: TransactionType;
  amount: number | string;
  transactionDate: string; // YYYY-MM-DD
  description?: string;
  clientOperationId?: string;
  version?: number;
  createdAt: string;
  updatedAt: string;
}

export interface TransactionFilters {
  type?: TransactionType;
  accountId?: string;
  categoryId?: string;
  fromDate?: string; // YYYY-MM-DD
  toDate?: string; // YYYY-MM-DD
  page?: number;
  size?: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  last: boolean;
  first: boolean;
  empty: boolean;
}
