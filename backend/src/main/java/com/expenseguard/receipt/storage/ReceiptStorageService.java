package com.expenseguard.receipt.storage;

import java.nio.file.Path;
import java.util.UUID;

/**
 * Storage service abstraction for securely persisting and retrieving uploaded receipt files.
 */
public interface ReceiptStorageService {

    /**
     * Stores raw bytes for a user's receipt and returns a generated storage key.
     */
    String storeFile(UUID userId, String originalFilename, byte[] content);

    /**
     * Returns Path to the stored receipt file on disk.
     */
    Path loadFilePath(UUID userId, String storageKey);

    /**
     * Deletes the stored receipt file from disk.
     */
    void deleteFile(UUID userId, String storageKey);

    /**
     * Checks if the stored receipt file exists on disk.
     */
    boolean exists(UUID userId, String storageKey);
}
