package com.eventhub.dto;

import com.eventhub.domain.UserRole;

import java.util.UUID;

/**
 * API representation of a user, excluding sensitive fields like the password.
 */
public record UserResponseDTO(
        UUID id,
        String name,
        String email,
        UserRole role
) {
}
