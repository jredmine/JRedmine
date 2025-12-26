package com.github.jredmine.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 密码重置请求DTO
 *
 * @author panfeng
 */
@Data
public class PasswordResetRequestDTO {

    @NotBlank(message = "邮箱地址是必填项")
    @Email(message = "邮箱地址格式不正确")
    private String email;
}

