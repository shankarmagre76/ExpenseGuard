package com.expenseguard.analytics.service;

import com.expenseguard.analytics.dto.*;

/**
 * Service interface for calculating server-side financial analytics and insights.
 */
public interface AnalyticsService {

    /**
     * Get overall monthly financial analytics (income, expense, net savings, savings rate).
     */
    MonthlyAnalyticsResponse getMonthlyAnalytics(String monthStr);

    /**
     * Get category-wise expense breakdown with percentages.
     */
    CategoryAnalyticsResponse getCategoryAnalytics(String monthStr);

    /**
     * Get account-wise cash flow breakdown.
     */
    AccountAnalyticsResponse getAccountAnalytics(String monthStr);

    /**
     * Get top spending categories up to specified limit.
     */
    TopCategoryResponse getTopCategories(String monthStr, Integer limit);

    /**
     * Get budget vs actual spending performance for a given month.
     */
    BudgetPerformanceResponse getBudgetPerformance(String monthStr);

    /**
     * Get multi-month financial trend analytics over date range.
     */
    MonthlyTrendResponse getMonthlyTrend(String fromMonthStr, String toMonthStr);
}
