package com.eventhub.exception;

/**
 * Thrown when attempting to create a user with an email that is already registered.
 * Handled by {@link GlobalExceptionHandler}, which maps it to a 409 response.
 */
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}
