package com.github.jredmine.dto.response.user;

import lombok.Data;

/**
 * 用户偏好设置响应DTO
 *
 * @author panfeng
 */
@Data
public class UserPreferenceResponseDTO {
    private Long userId;
    private Boolean hideMail;
    private String timeZone;
    private String others;
}

