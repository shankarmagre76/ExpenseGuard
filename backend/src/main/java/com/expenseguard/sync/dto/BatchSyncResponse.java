package com.expenseguard.sync.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response container DTO for batch synchronization results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSyncResponse {

    private List<SyncTransactionResponse> results;
}
