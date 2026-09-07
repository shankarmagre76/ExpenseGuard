package com.expenseguard.receipt.service;

import com.expenseguard.receipt.dto.ReceiptConfirmationRequest;
import com.expenseguard.receipt.dto.ReceiptResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Service interface managing receipt upload, file validation, storage, OCR processing,
 * confirmation updates, and physical file deletion.
 */
public interface ReceiptService {

    /**
     * Uploads and validates a receipt file, stores it, and triggers OCR processing.
     */
    ReceiptResponse uploadReceipt(MultipartFile file);

    /**
     * Retrieves all receipts owned by the authenticated user with pagination support.
     */
    Page<ReceiptResponse> getAllReceiptsForCurrentUser(Pageable pageable);

    /**
     * Retrieves specific receipt details by ID ensuring authenticated user ownership.
     */
    ReceiptResponse getReceiptById(UUID id);

    /**
     * Deletes a receipt record and deletes its physical file from disk.
     */
    void deleteReceipt(UUID id);

    /**
     * Triggers OCR extraction for an existing stored receipt.
     */
    ReceiptResponse processOcr(UUID id);

    /**
     * Confirms or corrects OCR extracted data on a receipt without creating a transaction.
     */
    ReceiptResponse confirmOcrData(UUID id, ReceiptConfirmationRequest request);
}
