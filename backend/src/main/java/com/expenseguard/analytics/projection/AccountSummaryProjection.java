package com.expenseguard.analytics.projection;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Spring Data JPA Projection for account cash flow aggregation queries.
 */
public interface AccountSummaryProjection {
    UUID getAccountId();
    String getAccountName();
    BigDecimal getTotalIncome();
    BigDecimal getTotalExpense();
}
