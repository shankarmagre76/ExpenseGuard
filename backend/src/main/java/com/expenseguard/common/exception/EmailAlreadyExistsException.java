package com.expenseguard.common.exception;

/**
 * Exception thrown when a user registration attempt uses an email address that is already registered.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
