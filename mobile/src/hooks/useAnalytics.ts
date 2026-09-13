import { useState, useCallback, useEffect } from 'react';
import {
  MonthlyAnalyticsResponse,
  CategoryAnalyticsResponse,
  AccountAnalyticsResponse,
  TopCategoryResponse,
  BudgetPerformanceResponse,
  MonthlyTrendResponse,
} from '../types/analytics';
import * as analyticsApi from '../api/endpoints/analyticsApi';
import { AppError } from '../types/api';
import { getCurrentMonth, getTrendRange } from '../utils/dateFormatter';

export const useAnalytics = (initialMonth: string = getCurrentMonth()) => {
  const [selectedMonth, setSelectedMonth] = useState<string>(initialMonth);
  const [monthlyAnalytics, setMonthlyAnalytics] = useState<MonthlyAnalyticsResponse | null>(null);
  const [categoryAnalytics, setCategoryAnalytics] = useState<CategoryAnalyticsResponse | null>(null);
  const [accountAnalytics, setAccountAnalytics] = useState<AccountAnalyticsResponse | null>(null);
  const [topCategories, setTopCategories] = useState<TopCategoryResponse | null>(null);
  const [budgetPerformance, setBudgetPerformance] = useState<BudgetPerformanceResponse | null>(null);
  const [monthlyTrend, setMonthlyTrend] = useState<MonthlyTrendResponse | null>(null);

  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAnalytics = useCallback(async (month: string) => {
    try {
      setLoading(true);
      setError(null);

      const trendRange = getTrendRange(month, 6);

      const [monthlyRes, categoryRes, accountRes, topCatRes, perfRes, trendRes] = await Promise.all([
        analyticsApi.getMonthlyAnalytics(month),
        analyticsApi.getCategoryAnalytics(month),
        analyticsApi.getAccountAnalytics(month),
        analyticsApi.getTopCategories(month, 5),
        analyticsApi.getBudgetPerformance(month),
        analyticsApi.getMonthlyTrend(trendRange.from, trendRange.to),
      ]);

      setMonthlyAnalytics(monthlyRes);
      setCategoryAnalytics(categoryRes);
      setAccountAnalytics(accountRes);
      setTopCategories(topCatRes);
      setBudgetPerformance(perfRes);
      setMonthlyTrend(trendRes);
    } catch (err: any) {
      const appErr = err as AppError;
      setError(appErr.message || 'Failed to load analytics data.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAnalytics(selectedMonth);
  }, [fetchAnalytics, selectedMonth]);

  const refreshAnalytics = useCallback(() => {
    return fetchAnalytics(selectedMonth);
  }, [fetchAnalytics, selectedMonth]);

  return {
    selectedMonth,
    setSelectedMonth,
    monthlyAnalytics,
    categoryAnalytics,
    accountAnalytics,
    topCategories,
    budgetPerformance,
    monthlyTrend,
    loading,
    error,
    refreshAnalytics,
  };
};
