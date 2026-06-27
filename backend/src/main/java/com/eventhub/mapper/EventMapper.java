package com.eventhub.mapper;

import com.eventhub.domain.Event;
import com.eventhub.dto.EventResponseDTO;
import org.mapstruct.Mapper;

/**
 * MapStruct mapper converting {@link Event} entities to API response DTOs.
 * Delegates organizer conversion to {@link UserMapper}.
 */
@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface EventMapper {

    /**
     * Converts an event entity into its response representation, including the organizer.
     */
    EventResponseDTO toEventResponseDTO(Event event);
}
