package com.eventhub.mapper;

import com.eventhub.domain.User;
import com.eventhub.dto.UserRequestDTO;
import com.eventhub.dto.UserResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "password", ignore = true)
    User toUser(UserRequestDTO dto);

    UserResponseDTO toUserResponseDTO(User user);
}
