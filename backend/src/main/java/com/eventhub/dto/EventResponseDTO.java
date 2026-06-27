package com.eventhub.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * API representation of an event, including its organizer's public details.
 */
public record EventResponseDTO(
        UUID id,
        String title,
        String description,
        OffsetDateTime eventDate,
        String location,
        UserResponseDTO organizer
) {
}
