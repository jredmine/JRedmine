package com.github.jredmine.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求DTO
 *
 * @author panfeng
 */
@Data
public class UserLoginRequestDTO {

    @NotBlank(message = "登录名是必填项")
    private String login;

    @NotBlank(message = "密码是必填项")
    private String password;
}

