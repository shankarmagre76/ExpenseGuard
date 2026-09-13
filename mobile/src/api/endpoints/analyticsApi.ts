import { apiClient } from '../client';
import {
  MonthlyAnalyticsResponse,
  CategoryAnalyticsResponse,
  AccountAnalyticsResponse,
  TopCategoryResponse,
  BudgetPerformanceResponse,
  MonthlyTrendResponse,
} from '../../types/analytics';

export const getMonthlyAnalytics = async (month?: string): Promise<MonthlyAnalyticsResponse> => {
  const params: Record<string, string> = {};
  if (month) params.month = month;
  return await apiClient.get<MonthlyAnalyticsResponse>('/api/v1/analytics/monthly', { params });
};

export const getCategoryAnalytics = async (month?: string): Promise<CategoryAnalyticsResponse> => {
  const params: Record<string, string> = {};
  if (month) params.month = month;
  return await apiClient.get<CategoryAnalyticsResponse>('/api/v1/analytics/categories', { params });
};

export const getAccountAnalytics = async (month?: string): Promise<AccountAnalyticsResponse> => {
  const params: Record<string, string> = {};
  if (month) params.month = month;
  return await apiClient.get<AccountAnalyticsResponse>('/api/v1/analytics/accounts', { params });
};

export const getTopCategories = async (month?: string, limit: number = 5): Promise<TopCategoryResponse> => {
  const params: Record<string, string | number> = { limit };
  if (month) params.month = month;
  return await apiClient.get<TopCategoryResponse>('/api/v1/analytics/top-categories', { params });
};

export const getBudgetPerformance = async (month?: string): Promise<BudgetPerformanceResponse> => {
  const params: Record<string, string> = {};
  if (month) params.month = month;
  return await apiClient.get<BudgetPerformanceResponse>('/api/v1/analytics/budget-performance', { params });
};

export const getMonthlyTrend = async (from?: string, to?: string): Promise<MonthlyTrendResponse> => {
  const params: Record<string, string> = {};
  if (from) params.from = from;
  if (to) params.to = to;
  return await apiClient.get<MonthlyTrendResponse>('/api/v1/analytics/trend', { params });
};
