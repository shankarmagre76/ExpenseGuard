package com.expenseguard.analytics.dto;

import com.expenseguard.budget.dto.BudgetResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for budget vs actual expense performance for a specified month.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetPerformanceResponse {
    private String month;
    private List<BudgetResponse> budgets;
    private BigDecimal totalBudgeted;
    private BigDecimal totalSpent;
    private BigDecimal overallUtilizationPercentage;
}
