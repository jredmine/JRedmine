package com.redmine.jredmine.mapper;

import com.redmine.jredmine.dto.UserRegisterRequestDTO;
import com.redmine.jredmine.dto.UserRegisterResponseDTO;
import com.redmine.jredmine.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class);

    UserRegisterResponseDTO toUserRegisterResponseDTO(User user);

    User toEntity(UserRegisterRequestDTO userRegisterRequestDTO);
}
