package com.github.jredmine.dto.converter;

import com.github.jredmine.dto.request.user.UserRegisterRequestDTO;
import com.github.jredmine.dto.response.user.UserDetailResponseDTO;
import com.github.jredmine.dto.response.user.UserListItemResponseDTO;
import com.github.jredmine.dto.response.user.UserRegisterResponseDTO;
import com.github.jredmine.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserConverter {
    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);

    UserRegisterResponseDTO toUserRegisterResponseDTO(User user);

    UserDetailResponseDTO toUserDetailResponseDTO(User user);

    UserListItemResponseDTO toUserListItemResponseDTO(User user);

    User toEntity(UserRegisterRequestDTO userRegisterRequestDTO);
}

