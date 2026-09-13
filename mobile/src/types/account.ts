/**
 * Account type definitions matching Spring Boot backend DTOs & AccountType enum.
 */

export type AccountType = 'CASH' | 'BANK' | 'SAVINGS' | 'CREDIT_CARD' | 'WALLET';

export interface AccountRequest {
  name: string;
  type: AccountType;
  openingBalance: number | string;
  currency?: string;
}

export interface AccountResponse {
  id: string;
  userId: string;
  name: string;
  type: AccountType;
  balance: number | string;
  currency: string;
  createdAt: string;
  updatedAt: string;
}
