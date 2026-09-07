package com.expenseguard.analytics.controller;

import com.expenseguard.analytics.dto.*;
import com.expenseguard.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller exposing financial analytics endpoints for the authenticated user.
 */
@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Get overall monthly financial summary (income, expense, net savings, savings rate).
     */
    @GetMapping("/monthly")
    public ResponseEntity<MonthlyAnalyticsResponse> getMonthlyAnalytics(
            @RequestParam(name = "month", required = false) String month) {
        MonthlyAnalyticsResponse response = analyticsService.getMonthlyAnalytics(month);
        return ResponseEntity.ok(response);
    }

    /**
     * Get category-wise expense breakdown with percentages.
     */
    @GetMapping("/categories")
    public ResponseEntity<CategoryAnalyticsResponse> getCategoryAnalytics(
            @RequestParam(name = "month", required = false) String month) {
        CategoryAnalyticsResponse response = analyticsService.getCategoryAnalytics(month);
        return ResponseEntity.ok(response);
    }

    /**
     * Get account-wise cash flow breakdown (total income, expense, net change per account).
     */
    @GetMapping("/accounts")
    public ResponseEntity<AccountAnalyticsResponse> getAccountAnalytics(
            @RequestParam(name = "month", required = false) String month) {
        AccountAnalyticsResponse response = analyticsService.getAccountAnalytics(month);
        return ResponseEntity.ok(response);
    }

    /**
     * Get top N spending categories for a specified month.
     */
    @GetMapping("/top-categories")
    public ResponseEntity<TopCategoryResponse> getTopCategories(
            @RequestParam(name = "month", required = false) String month,
            @RequestParam(name = "limit", required = false, defaultValue = "5") Integer limit) {
        TopCategoryResponse response = analyticsService.getTopCategories(month, limit);
        return ResponseEntity.ok(response);
    }

    /**
     * Get budget vs actual spending performance for a specified month.
     */
    @GetMapping("/budget-performance")
    public ResponseEntity<BudgetPerformanceResponse> getBudgetPerformance(
            @RequestParam(name = "month", required = false) String month) {
        BudgetPerformanceResponse response = analyticsService.getBudgetPerformance(month);
        return ResponseEntity.ok(response);
    }

    /**
     * Get multi-month financial trend analytics over a date range.
     */
    @GetMapping("/trend")
    public ResponseEntity<MonthlyTrendResponse> getMonthlyTrend(
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to) {
        MonthlyTrendResponse response = analyticsService.getMonthlyTrend(from, to);
        return ResponseEntity.ok(response);
    }
}
