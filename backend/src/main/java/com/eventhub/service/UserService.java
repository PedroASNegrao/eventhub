package com.eventhub.service;

import com.eventhub.domain.User;
import com.eventhub.dto.UserRequestDTO;
import com.eventhub.dto.UserResponseDTO;
import com.eventhub.exception.EmailAlreadyExistsException;
import com.eventhub.exception.ResourceNotFoundException;
import com.eventhub.mapper.UserMapper;
import com.eventhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Business logic for creating and retrieving users.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * Creates a new user, hashing the raw password before persistence.
     *
     * @throws com.eventhub.exception.EmailAlreadyExistsException if the email is already registered
     */
    @Transactional
    public UserResponseDTO createUser(UserRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new EmailAlreadyExistsException("Email already in use: " + dto.email());
        }

        User user = mapper.toUser(dto);
        user.setPassword(passwordEncoder.encode(dto.password()));

        userRepository.save(user);
        return mapper.toUserResponseDTO(user);
    }

    /**
     * Returns all users.
     */
    @Transactional(readOnly = true)
    public List<UserResponseDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(mapper::toUserResponseDTO)
                .toList();
    }

    /**
     * Returns a single user by id.
     *
     * @throws com.eventhub.exception.ResourceNotFoundException if no user matches the id
     */
    @Transactional(readOnly = true)
    public UserResponseDTO getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return mapper.toUserResponseDTO(user);
    }
}
