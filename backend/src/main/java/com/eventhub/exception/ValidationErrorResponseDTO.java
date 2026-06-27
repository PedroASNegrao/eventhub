package com.eventhub.exception;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Response body returned by {@code GlobalExceptionHandler} when a request fails
 * bean validation, mapping each invalid field to its corresponding error message.
 *
 * @param timestamp   when the error occurred
 * @param status      HTTP status code (400)
 * @param error       short HTTP status reason phrase (e.g. "Bad Request")
 * @param message     general description of the failure
 * @param path        request URI that triggered the validation error
 * @param fieldErrors map of invalid field names to their validation messages
 */
public record ValidationErrorResponseDTO(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
}
