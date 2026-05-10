package com.github.jredmine.dto.request.user;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 当前登录用户自助更新资料（不允许修改管理员标识、账号状态等）
 */
@Data
public class UserSelfUpdateRequestDTO {

    @Size(max = 30, message = "名字长度不能超过30个字符")
    private String firstname;

    @Size(max = 255, message = "姓氏长度不能超过255个字符")
    private String lastname;

    @Size(max = 255, message = "邮件地址长度不能超过255个字符")
    private String email;

    /**
     * 界面语言，如 zh-CN、en-US
     */
    private String language;

    /**
     * 邮件通知策略，如 all、none、only_my_events
     */
    private String mailNotification;
}
