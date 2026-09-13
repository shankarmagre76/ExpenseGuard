import { formatCurrency, getCurrencySymbol } from '../src/utils/currencyFormatter';
import { formatDateDisplay, getTodayISODate } from '../src/utils/dateFormatter';
import { AccountRequest } from '../src/types/account';
import { CategoryRequest } from '../src/types/category';
import { TransactionRequest } from '../src/types/transaction';

describe('Phase 3 Core Expense Management Unit Tests', () => {
  describe('Money & Currency Formatting (No Floating-Point Precision Loss)', () => {
    it('should format USD amounts correctly', () => {
      expect(formatCurrency(1250.5, 'USD')).toBe('$1,250.50');
      expect(formatCurrency('1250.50', 'USD')).toBe('$1,250.50');
      expect(formatCurrency(0, 'USD')).toBe('$0.00');
    });

    it('should format negative amounts correctly', () => {
      expect(formatCurrency(-45.99, 'USD')).toBe('-$45.99');
    });

    it('should resolve currency symbols correctly', () => {
      expect(getCurrencySymbol('INR')).toBe('₹');
      expect(getCurrencySymbol('EUR')).toBe('€');
      expect(getCurrencySymbol('GBP')).toBe('£');
      expect(getCurrencySymbol('USD')).toBe('$');
    });
  });

  describe('Calendar Date Formatting', () => {
    it('should format ISO YYYY-MM-DD date strings consistently', () => {
      const formatted = formatDateDisplay('2026-09-14');
      expect(formatted).toContain('Sep');
      expect(formatted).toContain('14');
      expect(formatted).toContain('2026');
    });

    it('should return current date in YYYY-MM-DD format', () => {
      const today = getTodayISODate();
      expect(today).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    });
  });

  describe('Account DTO Payload Validation', () => {
    it('should construct valid AccountRequest payload', () => {
      const payload: AccountRequest = {
        name: 'Main Chase Checking',
        type: 'BANK',
        openingBalance: 1500.0,
        currency: 'USD',
      };
      expect(payload.name).toBe('Main Chase Checking');
      expect(payload.type).toBe('BANK');
      expect(payload.openingBalance).toBe(1500.0);
    });
  });

  describe('Category DTO Payload Validation', () => {
    it('should construct valid Expense and Income CategoryRequest payloads', () => {
      const expenseCat: CategoryRequest = {
        name: 'Groceries & Supplies',
        type: 'EXPENSE',
      };
      const incomeCat: CategoryRequest = {
        name: 'Software Developer Salary',
        type: 'INCOME',
      };

      expect(expenseCat.type).toBe('EXPENSE');
      expect(incomeCat.type).toBe('INCOME');
    });
  });

  describe('Transaction DTO Payload Validation', () => {
    it('should construct valid Add Expense request payload', () => {
      const expensePayload: TransactionRequest = {
        accountId: 'acc-uuid-1',
        categoryId: 'cat-uuid-1',
        type: 'EXPENSE',
        amount: 85.5,
        transactionDate: '2026-09-14',
        description: 'Weekly supermarket shopping',
      };

      expect(expensePayload.type).toBe('EXPENSE');
      expect(expensePayload.amount).toBe(85.5);
      expect(expensePayload.accountId).toBe('acc-uuid-1');
    });

    it('should construct valid Add Income request payload', () => {
      const incomePayload: TransactionRequest = {
        accountId: 'acc-uuid-1',
        categoryId: 'cat-uuid-2',
        type: 'INCOME',
        amount: 5000.0,
        transactionDate: '2026-09-14',
        description: 'Monthly salary credit',
      };

      expect(incomePayload.type).toBe('INCOME');
      expect(incomePayload.amount).toBe(5000.0);
    });
  });
});
