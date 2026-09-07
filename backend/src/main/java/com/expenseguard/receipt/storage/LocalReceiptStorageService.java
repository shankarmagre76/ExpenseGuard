package com.expenseguard.receipt.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;

/**
 * File-system implementation of ReceiptStorageService storing receipts in a secure, isolated directory structure.
 */
@Slf4j
@Service
public class LocalReceiptStorageService implements ReceiptStorageService {

    private final Path rootStorageDir;

    public LocalReceiptStorageService(
            @Value("${expenseguard.receipt.storage-path:./uploads/receipts}") String storagePath) {
        this.rootStorageDir = Paths.get(storagePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootStorageDir);
            log.info("Initialized local receipt storage directory at: {}", this.rootStorageDir);
        } catch (IOException e) {
            log.error("Could not create receipt storage directory at: {}", this.rootStorageDir, e);
            throw new RuntimeException("Failed to initialize receipt storage directory", e);
        }
    }

    @Override
    public String storeFile(UUID userId, String originalFilename, byte[] content) {
        sanitizeUserIdAndFilename(userId, originalFilename);

        String storageKey = UUID.randomUUID().toString();
        Path userDir = rootStorageDir.resolve(userId.toString()).normalize();

        try {
            Files.createDirectories(userDir);
            Path destination = userDir.resolve(storageKey).normalize();

            // Prevent path traversal outside root storage dir
            if (!destination.startsWith(rootStorageDir)) {
                throw new IllegalArgumentException("Invalid file path attempted during storage");
            }

            Files.write(destination, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            log.debug("Saved receipt for user {} with storage key {}", userId, storageKey);
            return storageKey;
        } catch (IOException e) {
            log.error("Failed to store receipt file for user {}", userId, e);
            throw new RuntimeException("Failed to store receipt file", e);
        }
    }

    @Override
    public Path loadFilePath(UUID userId, String storageKey) {
        validateStorageKey(storageKey);
        Path userDir = rootStorageDir.resolve(userId.toString()).normalize();
        Path file = userDir.resolve(storageKey).normalize();

        if (!file.startsWith(rootStorageDir)) {
            throw new IllegalArgumentException("Invalid storage key path traversal detected");
        }
        return file;
    }

    @Override
    public void deleteFile(UUID userId, String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }
        try {
            Path file = loadFilePath(userId, storageKey);
            boolean deleted = Files.deleteIfExists(file);
            if (deleted) {
                log.info("Deleted stored receipt file: {}", file);
            }
        } catch (IOException e) {
            log.warn("Failed to delete stored receipt file for key {}: {}", storageKey, e.getMessage());
        }
    }

    @Override
    public boolean exists(UUID userId, String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return false;
        }
        try {
            Path file = loadFilePath(userId, storageKey);
            return Files.exists(file);
        } catch (Exception e) {
            return false;
        }
    }

    private void validateStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank() || storageKey.contains("..") || storageKey.contains("/") || storageKey.contains("\\")) {
            throw new IllegalArgumentException("Invalid storage key format");
        }
    }

    private void sanitizeUserIdAndFilename(UUID userId, String filename) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID must not be null");
        }
        if (filename != null && (filename.contains("..") || filename.contains("/") || filename.contains("\\"))) {
            log.warn("Potential path traversal detected in filename: {}", filename);
        }
    }
}
