package com.eventhub.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventResponseDTO(
        UUID id,
        String title,
        String description,
        OffsetDateTime eventDate,
        String location,
        UserResponseDTO organizer
) {
}
