package com.redmine.jredmine.controller;

import com.redmine.jredmine.dto.UserRegisterRequestDTO;
import com.redmine.jredmine.dto.UserRegisterResponseDTO;
import com.redmine.jredmine.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserRegisterResponseDTO> register(@Valid @RequestBody UserRegisterRequestDTO userRegisterRequestDTO, BindingResult bindingResult) throws Exception {
        if (bindingResult.hasErrors()) {
            //return ResponseEntity.badRequest().build();
            throw new Exception(Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage());
        }

        UserRegisterResponseDTO response = userService.register(userRegisterRequestDTO);
        return ResponseEntity.ok(response);
    }
}
