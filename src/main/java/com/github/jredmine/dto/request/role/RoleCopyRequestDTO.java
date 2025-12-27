package com.github.jredmine.dto.request.role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 角色复制请求DTO
 *
 * @author panfeng
 */
@Data
public class RoleCopyRequestDTO {

    @NotBlank(message = "角色名称是必填项")
    @Size(max = 255, message = "角色名称长度不能超过255个字符")
    private String name;
}

