package com.expenseguard.receipt.entity;

/**
 * Status of a uploaded receipt in the processing pipeline.
 */
public enum ReceiptStatus {
    UPLOADED,
    PROCESSING,
    PROCESSED,
    FAILED,
    CONFIRMED
}
