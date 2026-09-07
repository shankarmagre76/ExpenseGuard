package com.expenseguard.receipt.ocr;

import com.expenseguard.receipt.dto.OcrResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

/**
 * Deterministic test implementation of OcrService for predictable integration test runs.
 */
@Slf4j
@Service
@Profile("test")
public class TestOcrService implements OcrService {

    @Override
    public OcrResult processReceipt(Path filePath, String contentType) {
        log.info("TestOcrService processing test file: {}", filePath);
        try {
            String fileContent = "";
            if (filePath != null && Files.exists(filePath)) {
                fileContent = Files.readString(filePath);
            }

            if (fileContent.contains("FAILED_OCR") || fileContent.contains("CORRUPT")) {
                throw new RuntimeException("OCR engine failed to parse corrupt receipt file");
            }

            if (fileContent.contains("LOW_CONFIDENCE")) {
                return OcrResult.builder()
                        .merchantName("ABC Store")
                        .transactionDate(LocalDate.of(2026, 9, 7))
                        .amount(new BigDecimal("1250.00"))
                        .confidence(new BigDecimal("0.4500"))
                        .rawText(fileContent)
                        .build();
            }

            return OcrResult.builder()
                    .merchantName("Supermarket Mega Store")
                    .transactionDate(LocalDate.of(2026, 9, 7))
                    .amount(new BigDecimal("1250.00"))
                    .confidence(new BigDecimal("0.9500"))
                    .rawText("Supermarket Mega Store\nDate: 2026-09-07\nTotal: 1250.00")
                    .build();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Test OCR fallback due to unreadable binary or text file");
            return OcrResult.builder()
                    .merchantName("Supermarket Mega Store")
                    .transactionDate(LocalDate.of(2026, 9, 7))
                    .amount(new BigDecimal("1250.00"))
                    .confidence(new BigDecimal("0.9500"))
                    .rawText("Default Test Receipt Content")
                    .build();
        }
    }
}
