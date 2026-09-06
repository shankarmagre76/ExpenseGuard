package com.expenseguard.transaction.dto;

import com.expenseguard.transaction.entity.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object representing request payload for Transaction creation and updates.
 * Server-side security ignores any client-supplied owner or user IDs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    @NotNull(message = "Account ID is required")
    private UUID accountId;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be greater than zero")
    private BigDecimal amount;

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    @Size(max = 100, message = "Client operation ID cannot exceed 100 characters")
    private String clientOperationId;
}
