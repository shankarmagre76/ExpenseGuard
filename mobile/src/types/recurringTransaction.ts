import { TransactionType } from './transaction';

export type RecurrenceFrequency = 'DAILY' | 'WEEKLY' | 'MONTHLY' | 'YEARLY';

export interface RecurringTransactionRequest {
  accountId: string;
  categoryId: string;
  type: TransactionType;
  amount: number;
  description?: string;
  frequency: RecurrenceFrequency;
  startDate: string; // YYYY-MM-DD
  nextRunDate: string; // YYYY-MM-DD
  endDate?: string; // YYYY-MM-DD
}

export interface RecurringTransactionResponse {
  id: string;
  accountId: string;
  categoryId: string;
  categoryName: string;
  type: TransactionType;
  amount: number;
  description?: string;
  frequency: RecurrenceFrequency;
  startDate: string;
  nextRunDate: string;
  endDate?: string;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface RecurringTransactionStatusRequest {
  active: boolean;
}

export interface RecurringExecutionResponse {
  message: string;
  executedCount: number;
}
