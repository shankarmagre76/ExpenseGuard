package com.expenseguard.receipt.ocr;

import com.expenseguard.receipt.dto.OcrResult;

import java.nio.file.Path;

/**
 * Service interface for running OCR extraction on uploaded receipt files.
 */
public interface OcrService {

    /**
     * Processes receipt file at filePath and returns extracted OCR result metrics.
     *
     * @param filePath Path to the uploaded file on disk
     * @param contentType MIME type of the uploaded file
     * @return OcrResult DTO containing merchantName, transactionDate, amount, and confidence score.
     */
    OcrResult processReceipt(Path filePath, String contentType);
}
