package com.expenseguard.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for category-wise expense breakdown with percentages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryAnalyticsResponse {
    private String month;
    private BigDecimal totalExpense;
    private List<CategoryExpenseItem> categories;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryExpenseItem {
        private UUID categoryId;
        private String categoryName;
        private BigDecimal amount;
        private BigDecimal percentage;
    }
}
