package com.github.jredmine.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户新增请求DTO
 * 用于管理员创建新用户
 *
 * @author panfeng
 */
@Data
public class UserCreateRequestDTO {

    @NotBlank(message = "登录名是必填项")
    @Size(max = 255, message = "登录名长度不能超过255个字符")
    private String login;

    @NotBlank(message = "密码是必填项")
    @Size(min = 8, max = 40, message = "密码长度必须在8到40个字符之间")
    private String password;

    @NotBlank(message = "名字是必填项")
    @Size(max = 30, message = "名字长度不能超过30个字符")
    private String firstname;

    @NotBlank(message = "姓氏是必填项")
    @Size(max = 255, message = "姓氏长度不能超过255个字符")
    private String lastname;

    @NotBlank(message = "邮件地址是必填项")
    @Size(max = 255, message = "邮件地址长度不能超过255个字符")
    private String email;

    /**
     * 是否管理员（可选，默认为false）
     */
    private Boolean admin = false;

    /**
     * 用户状态（可选，默认为1-启用）
     * 1=启用, 2=锁定, 3=待激活等
     */
    private Integer status = 1;

    /**
     * 语言设置（可选，默认为zh-CN）
     */
    private String language = "zh-CN";

    /**
     * 邮件通知设置（可选，默认为all）
     */
    private String mailNotification = "all";
}

