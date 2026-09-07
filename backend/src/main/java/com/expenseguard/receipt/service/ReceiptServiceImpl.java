package com.expenseguard.receipt.service;

import com.expenseguard.auth.entity.User;
import com.expenseguard.auth.security.CurrentUserService;
import com.expenseguard.common.exception.ResourceAccessDeniedException;
import com.expenseguard.common.exception.ResourceNotFoundException;
import com.expenseguard.receipt.dto.OcrResult;
import com.expenseguard.receipt.dto.ReceiptConfirmationRequest;
import com.expenseguard.receipt.dto.ReceiptResponse;
import com.expenseguard.receipt.entity.Receipt;
import com.expenseguard.receipt.entity.ReceiptStatus;
import com.expenseguard.receipt.ocr.OcrService;
import com.expenseguard.receipt.repository.ReceiptRepository;
import com.expenseguard.receipt.storage.ReceiptStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * Service implementation managing receipt file validation, storage delegation,
 * OCR processing, user data confirmation, and secure deletion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptServiceImpl implements ReceiptService {

    public static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg", "image/jpg", "image/png", "application/pdf"
    );

    private final ReceiptRepository receiptRepository;
    private final ReceiptStorageService storageService;
    private final OcrService ocrService;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public ReceiptResponse uploadReceipt(MultipartFile file) {
        User currentUser = currentUserService.getCurrentUser();

        // 1. Validate File
        validateFile(file);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "receipt";
        String contentType = file.getContentType() != null ? file.getContentType().toLowerCase() : "image/jpeg";

        // 2. Store file on disk securely
        byte[] content;
        try {
            content = file.getBytes();
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to read uploaded file content");
        }

        String storageKey = storageService.storeFile(currentUser.getId(), originalFilename, content);

        // 3. Create initial Receipt entity
        Receipt receipt = Receipt.builder()
                .user(currentUser)
                .fileName(originalFilename)
                .contentType(contentType)
                .storageKey(storageKey)
                .fileSize(file.getSize())
                .status(ReceiptStatus.UPLOADED)
                .build();

        Receipt saved = receiptRepository.save(receipt);

        // 4. Process OCR synchronously for MVP
        processOcrInternal(saved);

        return mapToResponse(receiptRepository.save(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReceiptResponse> getAllReceiptsForCurrentUser(Pageable pageable) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        Page<Receipt> page = receiptRepository.findAllByUserId(currentUserId, pageable);
        return page.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ReceiptResponse getReceiptById(UUID id) {
        Receipt receipt = findAndVerifyOwnership(id);
        return mapToResponse(receipt);
    }

    @Override
    @Transactional
    public void deleteReceipt(UUID id) {
        Receipt receipt = findAndVerifyOwnership(id);
        storageService.deleteFile(receipt.getUser().getId(), receipt.getStorageKey());
        receiptRepository.delete(receipt);
        log.info("Deleted receipt ID {} and physical file for user {}", id, receipt.getUser().getId());
    }

    @Override
    @Transactional
    public ReceiptResponse processOcr(UUID id) {
        Receipt receipt = findAndVerifyOwnership(id);
        processOcrInternal(receipt);
        Receipt updated = receiptRepository.save(receipt);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public ReceiptResponse confirmOcrData(UUID id, ReceiptConfirmationRequest request) {
        Receipt receipt = findAndVerifyOwnership(id);

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Confirmed amount must be greater than zero");
        }

        if (request.getTransactionDate() == null) {
            throw new IllegalArgumentException("Confirmed transaction date is required");
        }

        receipt.setMerchantName(request.getMerchantName());
        receipt.setExtractedDate(request.getTransactionDate());
        receipt.setExtractedAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        receipt.setStatus(ReceiptStatus.CONFIRMED);

        Receipt updated = receiptRepository.save(receipt);
        log.info("Receipt ID {} OCR data confirmed by user {}", id, receipt.getUser().getId());
        return mapToResponse(updated);
    }

    private void processOcrInternal(Receipt receipt) {
        receipt.setStatus(ReceiptStatus.PROCESSING);
        try {
            Path filePath = storageService.loadFilePath(receipt.getUser().getId(), receipt.getStorageKey());
            OcrResult result = ocrService.processReceipt(filePath, receipt.getContentType());

            if (result != null) {
                receipt.setMerchantName(result.getMerchantName());
                receipt.setExtractedDate(result.getTransactionDate());
                if (result.getAmount() != null) {
                    receipt.setExtractedAmount(result.getAmount().setScale(2, RoundingMode.HALF_UP));
                }
                receipt.setConfidence(result.getConfidence());
                receipt.setStatus(ReceiptStatus.PROCESSED);
            } else {
                receipt.setStatus(ReceiptStatus.FAILED);
            }
        } catch (Exception e) {
            log.warn("OCR processing failed for receipt ID {}: {}", receipt.getId(), e.getMessage());
            receipt.setStatus(ReceiptStatus.FAILED);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required and cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds maximum allowed limit of 10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Unsupported file content type: " + contentType + ". Allowed types: image/jpeg, image/png, application/pdf"
            );
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && (originalFilename.contains("..") || originalFilename.contains("/") || originalFilename.contains("\\"))) {
            throw new IllegalArgumentException("Invalid filename format");
        }
    }

    private Receipt findAndVerifyOwnership(UUID id) {
        UUID currentUserId = currentUserService.getCurrentUserId();
        return receiptRepository.findByIdAndUserId(id, currentUserId)
                .orElseGet(() -> {
                    if (receiptRepository.existsById(id)) {
                        throw new ResourceAccessDeniedException("Access denied: You do not have permission to access this receipt");
                    }
                    throw new ResourceNotFoundException("Receipt not found with ID: " + id);
                });
    }

    private ReceiptResponse mapToResponse(Receipt receipt) {
        return ReceiptResponse.builder()
                .id(receipt.getId())
                .fileName(receipt.getFileName())
                .contentType(receipt.getContentType())
                .fileSize(receipt.getFileSize())
                .status(receipt.getStatus())
                .merchantName(receipt.getMerchantName())
                .extractedDate(receipt.getExtractedDate())
                .extractedAmount(receipt.getExtractedAmount())
                .confidence(receipt.getConfidence())
                .createdAt(receipt.getCreatedAt())
                .updatedAt(receipt.getUpdatedAt())
                .build();
    }
}
