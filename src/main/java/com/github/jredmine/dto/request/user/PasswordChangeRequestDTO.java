package com.github.jredmine.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户变更密码请求DTO
 *
 * @author panfeng
 */
@Data
public class PasswordChangeRequestDTO {

    @NotBlank(message = "旧密码是必填项")
    private String oldPassword;

    @NotBlank(message = "新密码是必填项")
    @Size(min = 8, max = 40, message = "新密码长度必须在8到40个字符之间")
    private String newPassword;

    @NotBlank(message = "确认新密码是必填项")
    @Size(min = 8, max = 40, message = "确认新密码长度必须在8到40个字符之间")
    private String confirmNewPassword;
}

