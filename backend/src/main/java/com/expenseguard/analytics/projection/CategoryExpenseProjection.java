package com.expenseguard.analytics.projection;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Spring Data JPA Projection for category expense aggregation queries.
 */
public interface CategoryExpenseProjection {
    UUID getCategoryId();
    String getCategoryName();
    BigDecimal getAmount();
}
