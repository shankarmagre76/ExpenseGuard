package com.expenseguard.receipt.controller;

import com.expenseguard.receipt.dto.ReceiptConfirmationRequest;
import com.expenseguard.receipt.dto.ReceiptResponse;
import com.expenseguard.receipt.service.ReceiptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST Controller exposing receipt upload, OCR extraction, confirmation, and file management APIs.
 */
@RestController
@RequestMapping("/api/v1/receipts")
@RequiredArgsConstructor
public class ReceiptController {

    private final ReceiptService receiptService;

    /**
     * Upload a new receipt document (JPEG, PNG, PDF <= 10MB) and run OCR extraction.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReceiptResponse> uploadReceipt(
            @RequestParam("file") MultipartFile file) {
        ReceiptResponse response = receiptService.uploadReceipt(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get paginated receipts for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<Page<ReceiptResponse>> getAllReceipts(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ReceiptResponse> responses = receiptService.getAllReceiptsForCurrentUser(pageable);
        return ResponseEntity.ok(responses);
    }

    /**
     * Get details for a specific receipt owned by user.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReceiptResponse> getReceiptById(@PathVariable("id") UUID id) {
        ReceiptResponse response = receiptService.getReceiptById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete a receipt record and remove its physical file from disk.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReceipt(@PathVariable("id") UUID id) {
        receiptService.deleteReceipt(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Trigger OCR re-processing for an existing receipt.
     */
    @PostMapping("/{id}/process")
    public ResponseEntity<ReceiptResponse> processOcr(@PathVariable("id") UUID id) {
        ReceiptResponse response = receiptService.processOcr(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Confirm or update OCR extracted data without creating a transaction.
     */
    @PutMapping("/{id}/confirm")
    public ResponseEntity<ReceiptResponse> confirmOcrData(
            @PathVariable("id") UUID id,
            @Valid @RequestBody ReceiptConfirmationRequest request) {
        ReceiptResponse response = receiptService.confirmOcrData(id, request);
        return ResponseEntity.ok(response);
    }
}
