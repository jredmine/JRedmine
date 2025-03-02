package com.github.jredmine.service;

import com.github.jredmine.dto.UserRegisterRequestDTO;
import com.github.jredmine.dto.UserRegisterResponseDTO;
import com.github.jredmine.entity.User;
import com.github.jredmine.mapper.UserMapper;
import com.github.jredmine.repository.UserRepository;
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
