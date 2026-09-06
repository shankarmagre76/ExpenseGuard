package com.expenseguard.common.exception;

/**
 * Exception thrown when a requested resource cannot be found in the system.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
