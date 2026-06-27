package com.eventhub.domain;

/**
 * Role assigned to a {@link User}, distinguishing who creates events from who
 * registers for them.
 */
public enum UserRole {
    ORGANIZER,
    ATTENDEE
}
