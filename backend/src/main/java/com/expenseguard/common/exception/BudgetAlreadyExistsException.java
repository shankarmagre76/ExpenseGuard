package com.expenseguard.common.exception;

/**
 * Exception thrown when creating or updating a budget results in a duplicate for the same user, category, and month.
 */
public class BudgetAlreadyExistsException extends RuntimeException {

    public BudgetAlreadyExistsException(String message) {
        super(message);
    }
}
