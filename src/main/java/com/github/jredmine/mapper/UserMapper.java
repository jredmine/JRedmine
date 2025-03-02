package com.github.jredmine.mapper;

import com.github.jredmine.dto.UserRegisterRequestDTO;
import com.github.jredmine.dto.UserRegisterResponseDTO;
import com.github.jredmine.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    UserRegisterResponseDTO toUserRegisterResponseDTO(User user);

    User toEntity(UserRegisterRequestDTO userRegisterRequestDTO);
}
