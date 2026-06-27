package com.eventhub.repository;

import com.eventhub.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository providing CRUD access to {@link User} entities,
 * plus email-based lookups used for login and uniqueness checks.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their unique email address.
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the given email is already registered.
     */
    boolean existsByEmail(String email);
}
