package com.github.jredmine.controller;

import com.github.jredmine.dto.request.user.UserRegisterRequestDTO;
import com.github.jredmine.dto.response.ApiResponse;
import com.github.jredmine.dto.response.user.UserRegisterResponseDTO;
import com.github.jredmine.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<UserRegisterResponseDTO> register(@Valid @RequestBody UserRegisterRequestDTO userRegisterRequestDTO) {
        UserRegisterResponseDTO response = userService.register(userRegisterRequestDTO);
        return ApiResponse.success("用户注册成功", response);
    }
}
