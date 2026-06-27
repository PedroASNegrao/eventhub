package com.eventhub.service;

import com.eventhub.domain.Event;
import com.eventhub.domain.User;
import com.eventhub.domain.UserRole;
import com.eventhub.dto.EventRequestDTO;
import com.eventhub.dto.EventResponseDTO;
import com.eventhub.dto.UserResponseDTO;
import com.eventhub.exception.ResourceNotFoundException;
import com.eventhub.mapper.EventMapper;
import com.eventhub.repository.EventRepository;
import com.eventhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EventService}, with repositories and mapper mocked out.
 */
@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventMapper mapper;

    @InjectMocks
    private EventService eventService;

    private User organizer;
    private EventRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        organizer = User.builder()
                .id(UUID.randomUUID())
                .name("Org Anizer")
                .email("organizer@example.com")
                .role(UserRole.ORGANIZER)
                .build();
        requestDTO = new EventRequestDTO(
                "Tech Conference",
                "A conference about tech",
                OffsetDateTime.now().plusDays(10),
                "Convention Center",
                organizer.getId()
        );
    }

    /**
     * Verifies that creating an event looks up the organizer, persists an event built
     * from the request fields, and returns the mapped response.
     */
    @Test
    void createEvent_buildsEventWithOrganizerAndReturnsResponse() {
        when(userRepository.findById(organizer.getId())).thenReturn(Optional.of(organizer));
        EventResponseDTO expectedResponse = new EventResponseDTO(
                UUID.randomUUID(), requestDTO.title(), requestDTO.description(), requestDTO.eventDate(),
                requestDTO.location(), new UserResponseDTO(organizer.getId(), organizer.getName(), organizer.getEmail(), organizer.getRole())
        );
        when(mapper.toEventResponseDTO(any(Event.class))).thenReturn(expectedResponse);

        EventResponseDTO result = eventService.createEvent(requestDTO);

        assertThat(result).isEqualTo(expectedResponse);

        ArgumentCaptor<Event> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(eventCaptor.capture());
        Event savedEvent = eventCaptor.getValue();
        assertThat(savedEvent.getTitle()).isEqualTo(requestDTO.title());
        assertThat(savedEvent.getDescription()).isEqualTo(requestDTO.description());
        assertThat(savedEvent.getEventDate()).isEqualTo(requestDTO.eventDate());
        assertThat(savedEvent.getLocation()).isEqualTo(requestDTO.location());
        assertThat(savedEvent.getOrganizer()).isEqualTo(organizer);
    }

    /**
     * Verifies that creating an event for a non-existent organizer fails fast without
     * persisting anything.
     */
    @Test
    void createEvent_throwsWhenOrganizerNotFound() {
        when(userRepository.findById(organizer.getId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.createEvent(requestDTO))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(organizer.getId().toString());

        verify(eventRepository, never()).save(any());
    }

    /**
     * Verifies that fetching an event by id returns the mapped response when found.
     */
    @Test
    void getEventById_returnsMappedEvent() {
        Event event = Event.builder().id(UUID.randomUUID()).title("Tech Conference").organizer(organizer).build();
        EventResponseDTO expectedResponse = new EventResponseDTO(
                event.getId(), event.getTitle(), null, null, null, null
        );
        when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
        when(mapper.toEventResponseDTO(event)).thenReturn(expectedResponse);

        EventResponseDTO result = eventService.getEventById(event.getId());

        assertThat(result).isEqualTo(expectedResponse);
    }

    /**
     * Verifies that fetching a non-existent event by id throws.
     */
    @Test
    void getEventById_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(eventRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getEventById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    /**
     * Verifies that fetching all events returns the mapped list from the repository.
     */
    @Test
    void getAllEvents_returnsMappedList() {
        Event event = Event.builder().id(UUID.randomUUID()).title("Tech Conference").organizer(organizer).build();
        EventResponseDTO expectedResponse = new EventResponseDTO(
                event.getId(), event.getTitle(), null, null, null, null
        );
        when(eventRepository.findAll()).thenReturn(List.of(event));
        when(mapper.toEventResponseDTO(event)).thenReturn(expectedResponse);

        List<EventResponseDTO> result = eventService.getAllEvents();

        assertThat(result).containsExactly(expectedResponse);
    }
}
