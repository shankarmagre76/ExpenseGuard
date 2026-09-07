package com.expenseguard.receipt.dto;

import com.expenseguard.receipt.entity.ReceiptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object representing safe Receipt response payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceiptResponse {

    private UUID id;
    private String fileName;
    private String contentType;
    private Long fileSize;
    private ReceiptStatus status;
    private String merchantName;
    private LocalDate extractedDate;
    private BigDecimal extractedAmount;
    private BigDecimal confidence;
    private Instant createdAt;
    private Instant updatedAt;
}
