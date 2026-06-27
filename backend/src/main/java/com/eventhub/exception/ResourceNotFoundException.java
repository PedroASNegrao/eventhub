package com.eventhub.exception;

/**
 * Thrown when a requested entity (user, event, etc.) cannot be found by id.
 * Handled by {@link GlobalExceptionHandler}, which maps it to a 404 response.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
