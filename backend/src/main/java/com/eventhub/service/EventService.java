package com.eventhub.service;

import com.eventhub.domain.Event;
import com.eventhub.domain.User;
import com.eventhub.dto.EventRequestDTO;
import com.eventhub.dto.EventResponseDTO;
import com.eventhub.exception.ResourceNotFoundException;
import com.eventhub.mapper.EventMapper;
import com.eventhub.repository.EventRepository;
import com.eventhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventMapper mapper;

    @Transactional
    public EventResponseDTO createEvent(EventRequestDTO dto) {
        User organizer = userRepository.findById(dto.organizerId())
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found with id: " + dto.organizerId()));

        Event event = Event.builder()
                .title(dto.title())
                .description(dto.description())
                .eventDate(dto.eventDate())
                .location(dto.location())
                .organizer(organizer)
                .build();

        eventRepository.save(event);
        return mapper.toEventResponseDTO(event);
    }

    @Transactional(readOnly = true)
    public List<EventResponseDTO> getAllEvents() {
        return eventRepository.findAll().stream()
                .map(mapper::toEventResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventResponseDTO getEventById(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + id));
        return mapper.toEventResponseDTO(event);
    }
}
