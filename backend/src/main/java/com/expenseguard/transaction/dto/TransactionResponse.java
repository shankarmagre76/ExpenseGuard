package com.expenseguard.transaction.dto;

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
 * Data Transfer Object representing Transaction response information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private UUID id;
    private UUID accountId;
    private UUID categoryId;
    private String categoryName;
    private TransactionType type;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private String description;
    private String clientOperationId;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
}
