package com.eventhub.dto;

import com.eventhub.domain.UserRole;

import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String name,
        String email,
        UserRole role
) {
}
