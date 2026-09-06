package com.expenseguard.transaction.dto;

import com.expenseguard.transaction.entity.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Data Transfer Object representing request payload for Account creation and updates.
 * Server-side security ignores any client-supplied owner or user IDs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountRequest {

    @NotBlank(message = "Account name is required")
    @Size(max = 100, message = "Account name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "Account type is required")
    private AccountType type;

    @NotNull(message = "Opening balance is required")
    private BigDecimal openingBalance;

    @Size(max = 3, message = "Currency code must not exceed 3 characters")
    private String currency;
}
