package com.eventhub.repository;

import com.eventhub.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Spring Data JPA repository providing CRUD access to {@link Event} entities.
 */
public interface EventRepository extends JpaRepository<Event, UUID> {
}
