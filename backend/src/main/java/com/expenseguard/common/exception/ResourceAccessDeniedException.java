package com.expenseguard.common.exception;

/**
 * Exception thrown when an authenticated user attempts to access or mutate a resource
 * that belongs to another user (IDOR prevention / resource ownership failure).
 */
public class ResourceAccessDeniedException extends RuntimeException {

    public ResourceAccessDeniedException(String message) {
        super(message);
    }
}
