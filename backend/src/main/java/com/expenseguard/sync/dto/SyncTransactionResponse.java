package com.expenseguard.sync.dto;

import com.expenseguard.sync.entity.SyncOperationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response DTO returned after processing a synchronization operation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncTransactionResponse {

    private String clientOperationId;
    private SyncOperationStatus status;
    private UUID transactionId;
    private Long serverVersion;
    private String errorCode;
    private String message;
}
