package com.eventhub.exception;

import java.time.OffsetDateTime;

/**
 * Generic error response body returned by {@link GlobalExceptionHandler} for
 * not-found, conflict, and unexpected-error cases.
 */
public record ErrorResponseDTO(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
