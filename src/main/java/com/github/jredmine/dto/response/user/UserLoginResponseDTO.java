package com.github.jredmine.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录响应DTO
 *
 * @author panfeng
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserLoginResponseDTO {
    /**
     * JWT Token
     */
    private String token;

    /**
     * Token类型（通常是Bearer）
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * Token过期时间（秒）
     */
    private Long expiresIn;

    /**
     * 用户基本信息
     */
    private UserInfo user;

    /**
     * 用户基本信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String login;
        private String firstname;
        private String lastname;
        private String email;
        private Boolean admin;
        private Integer status;
    }
}

