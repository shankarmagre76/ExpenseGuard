package com.expenseguard.recurring.dto;

import com.expenseguard.recurring.entity.RecurrenceFrequency;
import com.expenseguard.transaction.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object representing Recurring Transaction response payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransactionResponse {

    private UUID id;
    private UUID accountId;
    private UUID categoryId;
    private String categoryName;
    private TransactionType type;
    private BigDecimal amount;
    private String description;
    private RecurrenceFrequency frequency;
    private LocalDate startDate;
    private LocalDate nextRunDate;
    private LocalDate endDate;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
