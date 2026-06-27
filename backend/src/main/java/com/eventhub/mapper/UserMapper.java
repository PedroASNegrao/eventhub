package com.eventhub.mapper;

import com.eventhub.domain.User;
import com.eventhub.dto.UserRequestDTO;
import com.eventhub.dto.UserResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper converting between {@link User} entities and user DTOs.
 */
@Mapper(componentModel = "spring")
public interface UserMapper {

    /**
     * Converts a request DTO into a new user entity; the password is set separately
     * after hashing, so it is not mapped here.
     */
    @Mapping(target = "password", ignore = true)
    User toUser(UserRequestDTO dto);

    /**
     * Converts a user entity into its public response representation.
     */
    UserResponseDTO toUserResponseDTO(User user);
}
