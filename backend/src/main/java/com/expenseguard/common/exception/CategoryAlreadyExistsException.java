package com.expenseguard.common.exception;

/**
 * Exception thrown when creating or updating a category results in a duplicate for the same user and type.
 */
public class CategoryAlreadyExistsException extends RuntimeException {

    public CategoryAlreadyExistsException(String message) {
        super(message);
    }
}
