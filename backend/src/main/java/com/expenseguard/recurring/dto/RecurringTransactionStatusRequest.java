package com.expenseguard.recurring.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for toggling recurring transaction active status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringTransactionStatusRequest {

    @NotNull(message = "Active status is required")
    private Boolean active;
}
