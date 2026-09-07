package com.expenseguard.sync.dto;

import com.expenseguard.sync.entity.SyncEntityType;
import com.expenseguard.sync.entity.SyncOperationType;
import com.expenseguard.transaction.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO representing an offline synchronization operation for a transaction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncTransactionRequest {

    @NotBlank(message = "clientOperationId is required")
    @Size(max = 100, message = "clientOperationId must not exceed 100 characters")
    private String clientOperationId;

    @NotNull(message = "operationType is required")
    private SyncOperationType operationType;

    private SyncEntityType entityType;

    private UUID transactionId;

    private UUID accountId;

    private UUID categoryId;

    private TransactionType type;

    private BigDecimal amount;

    private LocalDate transactionDate;

    private String description;

    private Long version;
}
