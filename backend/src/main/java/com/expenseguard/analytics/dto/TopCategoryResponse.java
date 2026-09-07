package com.expenseguard.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for top N spending categories in a specified month.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopCategoryResponse {
    private String month;
    private List<CategoryAnalyticsResponse.CategoryExpenseItem> topCategories;
}
