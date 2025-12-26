package com.github.jredmine.dto.request.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户更新请求DTO
 * 用于更新用户信息
 *
 * @author panfeng
 */
@Data
public class UserUpdateRequestDTO {

    @Size(max = 30, message = "名字长度不能超过30个字符")
    private String firstname;

    @Size(max = 255, message = "姓氏长度不能超过255个字符")
    private String lastname;

    @Size(max = 255, message = "邮件地址长度不能超过255个字符")
    private String email;

    /**
     * 是否管理员（可选）
     */
    private Boolean admin;

    /**
     * 用户状态（可选）
     * 1=启用, 2=锁定, 3=待激活等
     */
    private Integer status;

    /**
     * 语言设置（可选）
     */
    private String language;

    /**
     * 邮件通知设置（可选）
     */
    private String mailNotification;
}

