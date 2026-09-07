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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default production OCR service implementation extracting receipt values using pattern matching heuristics.
 */
@Slf4j
@Service
@Profile("!test")
public class PatternOcrService implements OcrService {

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4}[-/.]\\d{2}[-/.]\\d{2})");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("(?:TOTAL|AMOUNT|BAL|SUM)[\\s:$]*([0-9]+\\.[0-9]{2})", Pattern.CASE_INSENSITIVE);

    @Override
    public OcrResult processReceipt(Path filePath, String contentType) {
        log.info("PatternOcrService processing file: {}", filePath);
        try {
            String text = "";
            if (filePath != null && Files.exists(filePath)) {
                text = Files.readString(filePath);
            }

            String merchantName = extractMerchantName(text);
            LocalDate date = extractDate(text);
            BigDecimal amount = extractAmount(text);
            BigDecimal confidence = calculateConfidence(merchantName, date, amount);

            return OcrResult.builder()
                    .merchantName(merchantName)
                    .transactionDate(date)
                    .amount(amount)
                    .confidence(confidence)
                    .rawText(text)
                    .build();
        } catch (Exception e) {
            log.warn("Pattern OCR parsing failed for file {}: {}", filePath, e.getMessage());
            return OcrResult.builder()
                    .merchantName(null)
                    .transactionDate(null)
                    .amount(null)
                    .confidence(BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP))
                    .rawText("")
                    .build();
        }
    }

    private String extractMerchantName(String text) {
        if (text == null || text.isBlank()) return "Unknown Merchant";
        String[] lines = text.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && trimmed.length() < 50 && !trimmed.matches(".*\\d{4}.*")) {
                return trimmed;
            }
        }
        return "Receipt Merchant";
    }

    private LocalDate extractDate(String text) {
        if (text == null) return LocalDate.now();
        Matcher matcher = DATE_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                String rawDate = matcher.group(1).replace('/', '-').replace('.', '-');
                return LocalDate.parse(rawDate);
            } catch (Exception e) {
                log.debug("Failed to parse extracted date string", e);
            }
        }
        return LocalDate.now();
    }

    private BigDecimal extractAmount(String text) {
        if (text == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        Matcher matcher = AMOUNT_PATTERN.matcher(text);
        if (matcher.find()) {
            try {
                return new BigDecimal(matcher.group(1)).setScale(2, RoundingMode.HALF_UP);
            } catch (Exception e) {
                log.debug("Failed to parse extracted amount string", e);
            }
        }
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateConfidence(String merchant, LocalDate date, BigDecimal amount) {
        double score = 0.3;
        if (merchant != null && !merchant.equals("Unknown Merchant")) score += 0.25;
        if (date != null) score += 0.25;
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) score += 0.20;
        return BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP);
    }
}
