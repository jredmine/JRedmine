package com.github.jredmine.dto.request.user;

import lombok.Data;

/**
 * 用户偏好设置更新请求DTO
 *
 * @author panfeng
 */
@Data
public class UserPreferenceUpdateRequestDTO {

    /**
     * 是否隐藏邮箱
     */
    private Boolean hideMail;

    /**
     * 时区（如：Asia/Shanghai）
     */
    private String timeZone;

    /**
     * 其他偏好设置（JSON格式）
     */
    private String others;
}

