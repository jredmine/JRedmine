package com.github.jredmine.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserRegisterRequestDTO {

    @NotBlank(message = "登录名是必填项")
    @Size(max = 255, message = "登录名长度不能超过255个字符")
    private String login;

    @NotBlank(message = "密码是必填项")
    @Size(min = 8, max = 40, message = "密码长度必须在8到40个字符之间")
    private String password;

    @NotBlank(message = "确认密码是必填项")
    @Size(min = 8, max = 40, message = "确认密码长度必须在8到40个字符之间")
    private String confirmPassword;

    @NotBlank(message = "名字是必填项")
    @Size(max = 30, message = "名字长度不能超过30个字符")
    private String firstname;

    @NotBlank(message = "姓氏是必填项")
    @Size(max = 255, message = "姓氏长度不能超过255个字符")
    private String lastname;

    @NotBlank(message = "邮件地址是必填项")
    @Size(max = 255, message = "邮件地址长度不能超过255个字符")
    private String email;

    private boolean hideEmailFlag;
}

