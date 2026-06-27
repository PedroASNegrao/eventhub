package com.eventhub.service;

import com.eventhub.domain.User;
import com.eventhub.domain.UserRole;
import com.eventhub.dto.UserRequestDTO;
import com.eventhub.dto.UserResponseDTO;
import com.eventhub.exception.EmailAlreadyExistsException;
import com.eventhub.exception.ResourceNotFoundException;
import com.eventhub.mapper.UserMapper;
import com.eventhub.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper mapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private UserRequestDTO requestDTO;
    private User user;

    @BeforeEach
    void setUp() {
        requestDTO = new UserRequestDTO("Jane Doe", "jane@example.com", "password123", UserRole.ATTENDEE);
        user = User.builder()
                .id(UUID.randomUUID())
                .name("Jane Doe")
                .email("jane@example.com")
                .password("encoded-password")
                .role(UserRole.ATTENDEE)
                .build();
    }

    @Test
    void createUser_savesEncodedPasswordAndReturnsResponse() {
        UserResponseDTO expectedResponse = new UserResponseDTO(user.getId(), user.getName(), user.getEmail(), user.getRole());
        when(userRepository.existsByEmail(requestDTO.email())).thenReturn(false);
        when(mapper.toUser(requestDTO)).thenReturn(user);
        when(passwordEncoder.encode(requestDTO.password())).thenReturn("encoded-password");
        when(mapper.toUserResponseDTO(user)).thenReturn(expectedResponse);

        UserResponseDTO result = userService.createUser(requestDTO);

        assertThat(result).isEqualTo(expectedResponse);
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        verify(userRepository).save(user);
    }

    @Test
    void createUser_throwsWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail(requestDTO.email())).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(requestDTO))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining(requestDTO.email());

        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserById_returnsMappedUser() {
        UserResponseDTO expectedResponse = new UserResponseDTO(user.getId(), user.getName(), user.getEmail(), user.getRole());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(mapper.toUserResponseDTO(user)).thenReturn(expectedResponse);

        UserResponseDTO result = userService.getUserById(user.getId());

        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void getUserById_throwsWhenNotFound() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void getAllUsers_returnsMappedList() {
        UserResponseDTO expectedResponse = new UserResponseDTO(user.getId(), user.getName(), user.getEmail(), user.getRole());
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(mapper.toUserResponseDTO(user)).thenReturn(expectedResponse);

        List<UserResponseDTO> result = userService.getAllUsers();

        assertThat(result).containsExactly(expectedResponse);
    }
}
