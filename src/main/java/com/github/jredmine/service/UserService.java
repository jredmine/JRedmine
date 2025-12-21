package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.jredmine.dto.request.user.UserRegisterRequestDTO;
import com.github.jredmine.dto.response.user.UserRegisterResponseDTO;
import com.github.jredmine.entity.User;
import com.github.jredmine.dto.converter.UserConverter;
import com.github.jredmine.mapper.user.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserMapper userMapper;

    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public UserRegisterResponseDTO register(UserRegisterRequestDTO requestDTO) throws Exception {
        // 使用 Lambda QueryWrapper 检查用户是否存在（类型安全）
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getLogin, requestDTO.getLogin());
        User existsUser = userMapper.selectOne(queryWrapper);
        
        if (existsUser != null) {
            throw new Exception("User already exists");
        }

        User user = new User();
        user.setLogin(requestDTO.getLogin());
        user.setHashedPassword(requestDTO.getPassword());
        user.setFirstname(requestDTO.getFirstname());
        user.setLastname(requestDTO.getLastname());
        userMapper.insert(user);

        return UserConverter.INSTANCE.toUserRegisterResponseDTO(user);
    }
}
