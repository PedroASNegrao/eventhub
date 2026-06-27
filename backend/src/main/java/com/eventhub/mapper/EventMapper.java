package com.eventhub.mapper;

import com.eventhub.domain.Event;
import com.eventhub.dto.EventResponseDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = UserMapper.class)
public interface EventMapper {

    EventResponseDTO toEventResponseDTO(Event event);
}
