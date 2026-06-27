package com.eventhub.exception;

import java.time.OffsetDateTime;
import java.util.Map;

public record ValidationErrorResponseDTO(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
}
