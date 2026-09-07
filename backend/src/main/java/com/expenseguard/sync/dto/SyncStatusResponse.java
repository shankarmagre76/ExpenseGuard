package com.expenseguard.sync.dto;

import com.expenseguard.sync.entity.SyncOperationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for sync operation status lookup by clientOperationId.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncStatusResponse {

    private String clientOperationId;
    private SyncOperationStatus status;
    private UUID transactionId;
    private String errorCode;
    private String errorMessage;
    private Instant createdAt;
    private Instant processedAt;
}
