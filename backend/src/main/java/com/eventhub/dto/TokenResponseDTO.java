package com.eventhub.dto;

/**
 * Response payload returned by login and refresh, carrying both tokens and
 * the access token's lifetime in seconds.
 */
public record TokenResponseDTO(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
}
