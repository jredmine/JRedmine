package com.github.jredmine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.jredmine.dto.UserRegisterRequestDTO;
import com.github.jredmine.dto.UserRegisterResponseDTO;
import com.github.jredmine.entity.User;
import com.github.jredmine.mapper.UserMapper;
import com.github.jredmine.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserRegisterResponseDTO register(UserRegisterRequestDTO requestDTO) throws Exception {
        // 使用 Lambda QueryWrapper 检查用户是否存在（类型安全）
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getLogin, requestDTO.getLogin());
        User existsUser = userRepository.selectOne(queryWrapper);
        
        if (existsUser != null) {
            throw new Exception("User already exists");
        }

        User user = new User();
        user.setLogin(requestDTO.getLogin());
        user.setHashedPassword(requestDTO.getPassword());
        user.setFirstname(requestDTO.getFirstname());
        user.setLastname(requestDTO.getLastname());
        userRepository.insert(user);

        return UserMapper.INSTANCE.toUserRegisterResponseDTO(user);
    }
}
