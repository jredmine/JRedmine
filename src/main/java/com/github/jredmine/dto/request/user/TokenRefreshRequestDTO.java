package com.github.jredmine.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Token刷新请求DTO
 *
 * @author panfeng
 */
@Data
public class TokenRefreshRequestDTO {

    @NotBlank(message = "Token是必填项")
    private String token;
}

