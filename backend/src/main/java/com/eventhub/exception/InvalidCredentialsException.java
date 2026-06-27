package com.eventhub.exception;

/**
 * Thrown when login credentials don't match or a refresh token is invalid/expired.
 * Handled by {@link GlobalExceptionHandler}, which maps it to a 401 response.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
