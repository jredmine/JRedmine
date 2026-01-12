package com.github.jredmine.dto.response.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户简单信息响应DTO
 *
 * @author panfeng
 * @since 2026-01-12
 */
@Data
@Schema(description = "用户简单信息")
public class UserSimpleResponseDTO {
    
    @Schema(description = "用户ID")
    private Long id;
    
    @Schema(description = "登录名")
    private String login;
    
    @Schema(description = "名字")
    private String firstname;
    
    @Schema(description = "姓氏")
    private String lastname;
}
