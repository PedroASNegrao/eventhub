package com.eventhub.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for exchanging a refresh token for a new access token.
 */
public record RefreshRequestDTO(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
