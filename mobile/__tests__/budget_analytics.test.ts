import { getBudgetStatus, getBudgetStatusLabel } from '../src/types/budget';
import {
  formatMonthDisplay,
  getPreviousMonth,
  getNextMonth,
  getTrendRange,
} from '../src/utils/dateFormatter';
import * as budgetApi from '../src/api/endpoints/budgetApi';
import * as analyticsApi from '../src/api/endpoints/analyticsApi';
import { apiClient } from '../src/api/client';

jest.mock('../src/api/client', () => ({
  apiClient: {
    get: jest.fn(),
    post: jest.fn(),
    put: jest.fn(),
    delete: jest.fn(),
  },
}));

describe('Budget & Analytics Thresholds and Utilities', () => {
  describe('getBudgetStatus & Labels', () => {
    it('returns OK for utilization < 80%', () => {
      expect(getBudgetStatus(0)).toBe('OK');
      expect(getBudgetStatus(50)).toBe('OK');
      expect(getBudgetStatus(79.9)).toBe('OK');
      expect(getBudgetStatusLabel('OK')).toBe('On track');
    });

    it('returns WARNING for utilization between 80% and 99.9%', () => {
      expect(getBudgetStatus(80)).toBe('WARNING');
      expect(getBudgetStatus(95)).toBe('WARNING');
      expect(getBudgetStatus(99.9)).toBe('WARNING');
      expect(getBudgetStatusLabel('WARNING')).toBe('Warning');
    });

    it('returns REACHED for utilization exactly 100%', () => {
      expect(getBudgetStatus(100)).toBe('REACHED');
      expect(getBudgetStatusLabel('REACHED')).toBe('Budget reached');
    });

    it('returns EXCEEDED for utilization > 100%', () => {
      expect(getBudgetStatus(100.1)).toBe('EXCEEDED');
      expect(getBudgetStatus(150)).toBe('EXCEEDED');
      expect(getBudgetStatusLabel('EXCEEDED')).toBe('Over budget');
    });
  });

  describe('Date & Month Formatting Utilities', () => {
    it('formats YYYY-MM into human-readable month display', () => {
      expect(formatMonthDisplay('2026-09')).toBe('September 2026');
      expect(formatMonthDisplay('2026-01')).toBe('January 2026');
      expect(formatMonthDisplay('invalid')).toBe('invalid');
    });

    it('navigates previous and next months across year boundaries', () => {
      expect(getPreviousMonth('2026-09')).toBe('2026-08');
      expect(getPreviousMonth('2026-01')).toBe('2025-12');
      expect(getNextMonth('2026-09')).toBe('2026-10');
      expect(getNextMonth('2026-12')).toBe('2027-01');
    });

    it('generates multi-month trend date range', () => {
      const range = getTrendRange('2026-09', 6);
      expect(range.to).toBe('2026-09');
      expect(range.from).toBe('2026-04');
    });
  });

  describe('Budget API Client Endpoints', () => {
    beforeEach(() => {
      jest.clearAllMocks();
    });

    it('fetches list of budgets with optional month filter', async () => {
      const mockBudgets = [
        {
          id: 'b1',
          categoryId: 'cat1',
          categoryName: 'Groceries',
          month: '2026-09',
          amount: 500,
          spentAmount: 250,
          remainingAmount: 250,
          utilizationPercentage: 50,
        },
      ];
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockBudgets);

      const res = await budgetApi.getBudgets('2026-09');
      expect(apiClient.get).toHaveBeenCalledWith('/api/v1/budgets', {
        params: { month: '2026-09' },
      });
      expect(res).toEqual(mockBudgets);
    });

    it('creates a new budget allocation', async () => {
      const req = { categoryId: 'cat1', amount: 500, month: '2026-09' };
      const created = { id: 'b1', ...req, spentAmount: 0, remainingAmount: 500, utilizationPercentage: 0 };
      (apiClient.post as jest.Mock).mockResolvedValueOnce(created);

      const res = await budgetApi.createBudget(req);
      expect(apiClient.post).toHaveBeenCalledWith('/api/v1/budgets', req);
      expect(res).toEqual(created);
    });

    it('deletes a budget by ID', async () => {
      (apiClient.delete as jest.Mock).mockResolvedValueOnce(null);

      await budgetApi.deleteBudget('b1');
      expect(apiClient.delete).toHaveBeenCalledWith('/api/v1/budgets/b1');
    });
  });

  describe('Analytics API Client Endpoints', () => {
    beforeEach(() => {
      jest.clearAllMocks();
    });

    it('fetches monthly analytics summary', async () => {
      const mockSummary = {
        month: '2026-09',
        totalIncome: 5000,
        totalExpense: 2000,
        netSavings: 3000,
        savingsRate: 60,
      };
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockSummary);

      const res = await analyticsApi.getMonthlyAnalytics('2026-09');
      expect(apiClient.get).toHaveBeenCalledWith('/api/v1/analytics/monthly', {
        params: { month: '2026-09' },
      });
      expect(res).toEqual(mockSummary);
    });

    it('fetches category spending analytics', async () => {
      const mockCategories = {
        month: '2026-09',
        totalExpense: 1000,
        categories: [
          { categoryId: 'cat1', categoryName: 'Food', amount: 600, percentage: 60 },
        ],
      };
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockCategories);

      const res = await analyticsApi.getCategoryAnalytics('2026-09');
      expect(apiClient.get).toHaveBeenCalledWith('/api/v1/analytics/categories', {
        params: { month: '2026-09' },
      });
      expect(res).toEqual(mockCategories);
    });

    it('fetches top spending categories with limit', async () => {
      const mockTop = {
        month: '2026-09',
        topCategories: [
          { categoryId: 'cat1', categoryName: 'Rent', amount: 1200, percentage: 50 },
        ],
      };
      (apiClient.get as jest.Mock).mockResolvedValueOnce(mockTop);

      const res = await analyticsApi.getTopCategories('2026-09', 5);
      expect(apiClient.get).toHaveBeenCalledWith('/api/v1/analytics/top-categories', {
        params: { month: '2026-09', limit: 5 },
      });
      expect(res).toEqual(mockTop);
    });
  });
});
