package com.redmine.jredmine.service;

import ch.qos.logback.core.util.MD5Util;
import com.redmine.jredmine.dto.UserRegisterRequestDTO;
import com.redmine.jredmine.dto.UserRegisterResponseDTO;
import com.redmine.jredmine.entity.User;
import com.redmine.jredmine.mapper.UserMapper;
import com.redmine.jredmine.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    //private final PasswordEncoder passwordEncoder;

    //public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    //    this.userRepository = userRepository;
    //    this.passwordEncoder = passwordEncoder;
    //}
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserRegisterResponseDTO register(UserRegisterRequestDTO requestDTO) throws Exception {

        // 检查用户是否存在
        Optional<User> existsUser = userRepository.findByLogin(requestDTO.getLogin());
        if (existsUser.isPresent()) {
            throw new Exception("User already exists");
        }

        User user = new User();
        user.setLogin(requestDTO.getLogin());
        user.setHashedPassword(requestDTO.getPassword());
        user.setFirstname(requestDTO.getFirstname());
        user.setLastname(requestDTO.getLastname());
        userRepository.save(user);

        return UserMapper.INSTANCE.toUserRegisterResponseDTO(user);
    }
}
