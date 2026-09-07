package com.expenseguard.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * DTO for account cash flow breakdown (income, expense, net change per account).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountAnalyticsResponse {
    private String month;
    private List<AccountCashflowItem> accounts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountCashflowItem {
        private UUID accountId;
        private String accountName;
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal netChange;
    }
}
