package com.eventhub.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Request payload for creating an event.
 */
public record EventRequestDTO(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        String description,

        @NotNull(message = "Event date is required")
        @FutureOrPresent(message = "Event date cannot be in the past")
        OffsetDateTime eventDate,

        @NotBlank(message = "Location is required")
        @Size(max = 255, message = "Location must be at most 255 characters")
        String location,

        @NotNull(message = "Organizer id is required")
        UUID organizerId
) {
}
