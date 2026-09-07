package com.expenseguard.sync.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request container DTO for batch synchronization operations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSyncRequest {

    @NotEmpty(message = "Operations list must not be empty")
    @Valid
    private List<SyncTransactionRequest> operations;
}
