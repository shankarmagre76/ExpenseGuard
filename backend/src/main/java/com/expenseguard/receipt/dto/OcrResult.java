package com.expenseguard.receipt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Object capturing raw text and structured fields extracted by OCR.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResult {
    private String merchantName;
    private LocalDate transactionDate;
    private BigDecimal amount;
    private BigDecimal confidence;
    private String rawText;
}
