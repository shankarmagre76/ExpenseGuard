package com.expenseguard.common.exception;

/**
 * Exception thrown when user authentication fails due to invalid email or password.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
